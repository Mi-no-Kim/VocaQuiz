import { BrowserRouter, Route, Routes } from "react-router";
import { AdminGate } from "@/components/AdminGate";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <AdminGate>
              <div className="p-6 text-sm text-muted-foreground">
                관리자 화면 준비 중입니다.
              </div>
            </AdminGate>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
