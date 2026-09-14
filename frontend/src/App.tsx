import { BrowserRouter, Route, Routes } from "react-router";
import { AdminGate } from "@/components/AdminGate";
import { AdminVideosPage } from "@/pages/AdminVideosPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <AdminGate>
              <AdminVideosPage />
            </AdminGate>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
