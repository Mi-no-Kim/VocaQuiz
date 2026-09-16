import { BrowserRouter, Route, Routes } from "react-router";
import { AdminGate } from "@/components/AdminGate";
import { AdminSongNewPage } from "@/pages/AdminSongNewPage";
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
        <Route
          path="/admin/songs/new"
          element={
            <AdminGate>
              <AdminSongNewPage />
            </AdminGate>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
