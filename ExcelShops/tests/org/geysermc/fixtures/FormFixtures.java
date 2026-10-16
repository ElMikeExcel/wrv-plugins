// SPDX-License-Identifier: MIT
package org.geysermc.fixtures;

import java.util.*;
import java.util.function.BiConsumer;
import org.geysermc.cumulus.form.*;
import org.geysermc.cumulus.response.*;

/** Small original fixtures with nonpublic builders, forms and responses to expose illegal reflection. */
public final class FormFixtures {
    private FormFixtures() { }
    public static final List<State> built = new ArrayList<>();
    public static boolean throwOnValidHandler, throwOnClosedHandler, throwOnBuild, throwOnResponse;
    public static void reset() {
        built.clear(); throwOnValidHandler = false; throwOnClosedHandler = false; throwOnBuild = false; throwOnResponse = false;
    }
    public static final class State {
        public String title, text, placeholder, initial;
        public final List<String> buttons = new ArrayList<>();
        public BiConsumer<Object,Object> valid;
        public Runnable closed;
        public Form form;
        public void number(String value) { valid.accept(form, new HiddenNumberResponse(value)); }
        public void choice(int value) { valid.accept(form, new HiddenChoiceResponse(value)); }
        public void invalid() { closed.run(); }
        public void close() { closed.run(); }
    }
    public static State last() { return built.getLast(); }
    public static CustomForm.Builder customBuilder() { return new HiddenCustomBuilder(); }
    public static SimpleForm.Builder simpleBuilder() { return new HiddenSimpleBuilder(); }
    private record HiddenCustomForm() implements CustomForm { }
    private record HiddenSimpleForm() implements SimpleForm { }
    private record HiddenNumberResponse(String input) implements CustomFormResponse {
        public String asInput(int index) {
            if (throwOnResponse || index != 0) throw new IllegalArgumentException("Invalid fixture response");
            return input;
        }
    }
    private record HiddenChoiceResponse(int value) implements SimpleFormResponse {
        public int clickedButtonId() {
            if (throwOnResponse) throw new IllegalArgumentException("Invalid fixture response");
            return value;
        }
    }
    private static final class HiddenCustomBuilder implements CustomForm.Builder {
        private final State state = new State();
        public CustomForm.Builder title(String title) { state.title = title; return this; }
        public CustomForm.Builder input(String text, String placeholder, String initial) {
            state.text = text; state.placeholder = placeholder; state.initial = initial; return this;
        }
        public CustomForm.Builder validResultHandler(BiConsumer<CustomForm,CustomFormResponse> handler) {
            if (throwOnValidHandler) throw new UnsupportedOperationException("Valid handler unsupported");
            state.valid = (form, response) -> handler.accept((CustomForm) form, (CustomFormResponse) response); return this;
        }
        public CustomForm.Builder closedOrInvalidResultHandler(Runnable handler) {
            if (throwOnClosedHandler) throw new UnsupportedOperationException("Close handler unsupported");
            state.closed = handler; return this;
        }
        public CustomForm build() {
            if (throwOnBuild) throw new IllegalStateException("Fixture build failure");
            HiddenCustomForm form = new HiddenCustomForm(); state.form = form; built.add(state); return form;
        }
    }
    private static final class HiddenSimpleBuilder implements SimpleForm.Builder {
        private final State state = new State();
        public SimpleForm.Builder title(String title) { state.title = title; return this; }
        public SimpleForm.Builder content(String content) { state.text = content; return this; }
        public SimpleForm.Builder button(String label) { state.buttons.add(label); return this; }
        public SimpleForm.Builder validResultHandler(BiConsumer<SimpleForm,SimpleFormResponse> handler) {
            if (throwOnValidHandler) throw new UnsupportedOperationException("Valid handler unsupported");
            state.valid = (form, response) -> handler.accept((SimpleForm) form, (SimpleFormResponse) response); return this;
        }
        public SimpleForm.Builder closedOrInvalidResultHandler(Runnable handler) {
            if (throwOnClosedHandler) throw new UnsupportedOperationException("Close handler unsupported");
            state.closed = handler; return this;
        }
        public SimpleForm build() {
            if (throwOnBuild) throw new IllegalStateException("Fixture build failure");
            HiddenSimpleForm form = new HiddenSimpleForm(); state.form = form; built.add(state); return form;
        }
    }
}
