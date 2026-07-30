import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { CreatePage, LookupPage, OrderDetailPage, OrdersPage } from './pages'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<OrdersPage />} />
        <Route path="/new" element={<CreatePage />} />
        <Route path="/lookup" element={<LookupPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
