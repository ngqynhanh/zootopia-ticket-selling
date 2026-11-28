import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="bg-zoo-green-dark text-gray-200 py-10 border-t border-green-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-sm">
          {/* Cột 1: Giới thiệu */}
          <div>
            <h3 className="text-ticket-gold font-bold text-base mb-3">ZooPass</h3>
            <p className="text-gray-300 leading-relaxed">
              Nền tảng đặt vé tham quan trực tuyến, đăng ký khách đoàn ưu đãi và lên kế hoạch
              khám phá sở thú.
            </p>
            <p className="mt-2 text-xs text-gray-400">
              * Đồ án môn Web Application Development — dữ liệu chỉ dùng cho mục đích học tập.
            </p>
          </div>

          {/* Cột 2: Giờ hoạt động */}
          <div>
            <h3 className="text-ticket-gold font-bold text-base mb-3">Giờ hoạt động</h3>
            <p>Mở cửa hằng ngày (kể cả Thứ 7, CN &amp; Ngày lễ)</p>
            <p className="text-white font-semibold mt-1">07:00 – 18:30</p>
            <p className="text-xs text-gray-400 mt-1">
              Ngưng bán vé online cho ngày hiện tại lúc 17:30.
            </p>
          </div>

          {/* Cột 3: Liên kết nhanh */}
          <div>
            <h3 className="text-ticket-gold font-bold text-base mb-3">Liên kết nhanh</h3>
            <ul className="space-y-2">
              <li>
                <Link to="/tickets" className="hover:text-ticket-gold transition-colors">
                  Bảng giá vé cổng &amp; dịch vụ
                </Link>
              </li>
              <li>
                <Link to="/combos" className="hover:text-ticket-gold transition-colors">
                  Combo trải nghiệm
                </Link>
              </li>
              <li>
                <Link to="/group-booking" className="hover:text-ticket-gold transition-colors">
                  Chính sách &amp; Đặt vé đoàn
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-8 pt-4 border-t border-green-800 text-center text-xs text-gray-400">
          © {new Date().getFullYear()} ZooPass Team. All rights reserved.
        </div>
      </div>
    </footer>
  );
}