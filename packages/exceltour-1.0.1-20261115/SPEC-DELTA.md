# SPEC DELTA - ExcelTour 1.0.1 (BB-09)
Rows closed (audit verdict No -> fixed):
1. Guide view TourMenu - Java - FIX-2: slot 26 Close barrier
   -> Back to Server Menu (all states).
2. Guide view TourMenu as Bedrock fallback - FIX-2: same
   inventory, same fix (one code path).
3. Tour step form (Presentation.sendStepForm) - Bedrock -
   FIX-2: gains last button Back to Server Menu (backmenu).
Out of scope, unchanged: Tour step presentation Java (action
bar + chat) - audit Not-a-defect (chat-only surface).
Invariant added: Back = pure navigation (no engine call, no
save); tour progress survives a mid-tour Back (BackNavTest).
