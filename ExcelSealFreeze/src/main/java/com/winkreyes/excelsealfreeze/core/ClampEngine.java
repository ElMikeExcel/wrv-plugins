package com.winkreyes.excelsealfreeze.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC clamp engine over EliteMobs' player_data.db (table PlayerData).
 * Works on any open SQLite connection; the host owns connection handling,
 * busy timeouts and scheduling. Only Progression.COLUMNS are ever read or
 * written. All writes for a cycle run inside one transaction; any failure
 * rolls the whole cycle back, so a half-clamped database is impossible.
 */
public final class ClampEngine {

    public static final class Report {
        public int rowsRead;
        public int rowsClamped;
        public int columnsClamped;

        @Override
        public String toString() {
            return "rowsRead=" + rowsRead + " rowsClamped=" + rowsClamped
                    + " columnsClamped=" + columnsClamped;
        }
    }

    private ClampEngine() {
    }

    private static String selectSql() {
        StringBuilder sb = new StringBuilder("SELECT PlayerUUID");
        for (String col : Progression.COLUMNS) sb.append(", \"").append(col).append('"');
        sb.append(" FROM PlayerData");
        return sb.toString();
    }

    public static Map<String, Progression.ProgRow> readAll(Connection c) throws SQLException {
        Map<String, Progression.ProgRow> out = new LinkedHashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(selectSql())) {
            while (rs.next()) {
                out.put(rs.getString(1), readRow(rs));
            }
        }
        return out;
    }

    public static Progression.ProgRow readOne(Connection c, String uuid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(selectSql() + " WHERE PlayerUUID = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readRow(rs) : null;
            }
        }
    }

    private static Progression.ProgRow readRow(ResultSet rs) throws SQLException {
        long[] skill = new long[Progression.SKILL_COLUMNS.length];
        for (int i = 0; i < skill.length; i++) {
            skill[i] = rs.getLong(7 + i);
        }
        return new Progression.ProgRow(
                rs.getString(1),
                rs.getLong(2),
                rs.getLong(3),
                rs.getLong(4),
                rs.getDouble(5),
                rs.getLong(6),
                skill);
    }

    /** Clamp every row against the baseline. Runs in one transaction on c. */
    public static Report clampAll(Connection c, Map<String, Progression.ProgRow> baseline) throws SQLException {
        Map<String, Progression.ProgRow> rows = readAll(c);
        Report report = new Report();
        report.rowsRead = rows.size();
        apply(c, baseline, rows.values(), report);
        return report;
    }

    /** Clamp a single player's row (join/quit enforcement). */
    public static Report clampOne(Connection c, Map<String, Progression.ProgRow> baseline, String uuid) throws SQLException {
        Report report = new Report();
        Progression.ProgRow row = readOne(c, uuid);
        if (row == null) return report;
        report.rowsRead = 1;
        apply(c, baseline, List.of(row), report);
        return report;
    }

    private static void apply(Connection c, Map<String, Progression.ProgRow> baseline,
                              Iterable<Progression.ProgRow> rows, Report report) throws SQLException {
        boolean wasAutoCommit = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            for (Progression.ProgRow row : rows) {
                List<ClampPolicy.Clamp> clamps = ClampPolicy.diff(baseline.get(row.uuid), row);
                if (clamps.isEmpty()) continue;
                String sql = ClampPolicy.updateSql(clamps);
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int idx = 1;
                    for (ClampPolicy.Clamp clamp : clamps) {
                        if (clamp.isDouble) ps.setDouble(idx++, clamp.doubleValue);
                        else ps.setLong(idx++, clamp.longValue);
                    }
                    ps.setString(idx, row.uuid);
                    ps.executeUpdate();
                }
                report.rowsClamped++;
                report.columnsClamped += clamps.size();
            }
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // The original failure is the one that matters.
            }
            throw e;
        } finally {
            c.setAutoCommit(wasAutoCommit);
        }
    }
}
