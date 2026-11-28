import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import { Layout } from './components/layout/Layout';

function PagePlaceholder({ title, description }: { title: string; description: string }) {
  return (
    <section className="bg-white p-6 sm:p-8 rounded-xl shadow-sm border border-gray-200">
      <h1 className="text-2xl font-bold text-zoo-green">{title}</h1>
      <p className="mt-2 text-gray-600">{description}</p>
    </section>
  );
}

function NotFoundPage() {
  return (
    <section className="bg-white p-8 rounded-xl shadow-sm border border-gray-200 text-center max-w-lg mx-auto">
      <p className="text-5xl font-extrabold text-zoo-green">404</p>
      <h1 className="mt-3 text-xl font-bold text-gray-900">Không tìm thấy trang</h1>
      <p className="mt-2 text-sm text-gray-600">
        Đường dẫn bạn truy cập không tồn tại hoặc đã được thay đổi.
      </p>
      <Link
        to="/"
        className="inline-block mt-6 px-5 py-2.5 rounded-lg bg-zoo-green text-white text-sm font-semibold hover:bg-zoo-green-dark transition-colors"
      >
        Quay về Trang chủ
      </Link>
    </section>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route
            path="/"
            element={
              <PagePlaceholder
                title="Chào mừng đến với ZooPass"
                description="Hệ thống đặt vé online, đăng ký vé đoàn ưu đãi và lên kế hoạch tham quan."
              />
            }
          />
          <Route
            path="/tickets"
            element={
              <PagePlaceholder
                title="Bảng giá vé"
                description="Danh sách vé cổng và dịch vụ tham quan sẽ được kết nối API ở bước kế tiếp."
              />
            }
          />
          <Route
            path="/combos"
            element={
              <PagePlaceholder
                title="Combo trải nghiệm"
                description="Khám phá các gói combo trải nghiệm đặc sắc."
              />
            }
          />
          <Route
            path="/group-booking"
            element={
              <PagePlaceholder
                title="Vé đoàn & Đối tượng ưu đãi"
                description="Đặt vé cho đoàn từ 10 khách, hỗ trợ upload minh chứng và đặt cọc 30%."
              />
            }
          />
          <Route
            path="/login"
            element={
              <PagePlaceholder
                title="Đăng nhập"
                description="Biểu mẫu đăng nhập JWT sẽ được triển khai ở nhánh feature/fe-auth."
              />
            }
          />
          <Route
            path="/register"
            element={
              <PagePlaceholder
                title="Đăng ký tài khoản"
                description="Quy trình đăng ký 3 bước xác thực email sẽ được triển khai ở nhánh feature/fe-auth."
              />
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}

export default App;