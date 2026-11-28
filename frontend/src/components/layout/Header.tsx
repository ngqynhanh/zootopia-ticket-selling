import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { Menu, Ticket, X } from 'lucide-react';

const navLinks = [
  { to: '/', label: 'Trang chủ' },
  { to: '/tickets', label: 'Bảng giá vé' },
  { to: '/combos', label: 'Combo trải nghiệm' },
  { to: '/group-booking', label: 'Vé đoàn & Ưu đãi' },
];

export function Header() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const desktopLinkClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
      isActive
        ? 'bg-zoo-green-dark text-ticket-gold'
        : 'text-gray-100 hover:bg-zoo-green-dark/70 hover:text-ticket-gold'
    }`;

  const mobileLinkClass = ({ isActive }: { isActive: boolean }) =>
    `block px-3 py-2.5 rounded-md text-sm font-medium transition-colors ${
      isActive
        ? 'bg-zoo-green text-ticket-gold'
        : 'text-gray-100 hover:bg-zoo-green/80'
    }`;

  return (
    <header className="bg-zoo-green text-white shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Brand Logo */}
          <Link
            to="/"
            className="flex items-center gap-2.5 focus:outline-none"
            onClick={() => setIsMobileMenuOpen(false)}
          >
            <Ticket className="h-7 w-7 text-ticket-gold" aria-hidden="true" />
            <div className="flex flex-col leading-tight">
              <span className="text-xl font-extrabold tracking-wide text-ticket-gold">
                ZooPass
              </span>
              <span className="text-[11px] text-gray-200 hidden sm:inline">
                Đặt vé &amp; Lên kế hoạch tham quan
              </span>
            </div>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-1" aria-label="Điều hướng chính">
            {navLinks.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={desktopLinkClass}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          {/* Desktop Auth Actions */}
          <div className="hidden md:flex items-center gap-2">
            <Link
              to="/login"
              className="px-3.5 py-2 text-sm font-medium text-white hover:text-ticket-gold transition-colors"
            >
              Đăng nhập
            </Link>
            <Link
              to="/register"
              className="px-4 py-2 text-sm font-semibold rounded-lg bg-ticket-gold text-zoo-green-dark hover:bg-amber-400 transition-colors"
            >
              Đăng ký
            </Link>
          </div>

          {/* Mobile Hamburger Button */}
          <div className="md:hidden flex items-center">
            <button
              type="button"
              onClick={() => setIsMobileMenuOpen((prev) => !prev)}
              className="p-2 rounded-md text-gray-100 hover:bg-zoo-green-dark focus:outline-none focus:ring-2 focus:ring-ticket-gold"
              aria-label="Mở menu điều hướng"
              aria-expanded={isMobileMenuOpen}
            >
              {isMobileMenuOpen ? (
                <X className="h-6 w-6" aria-hidden="true" />
              ) : (
                <Menu className="h-6 w-6" aria-hidden="true" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Dropdown Menu */}
      {isMobileMenuOpen && (
        <nav
          className="md:hidden bg-zoo-green-dark px-4 pt-2 pb-4 space-y-1 border-t border-green-800"
          aria-label="Menu di động"
        >
          {navLinks.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={mobileLinkClass}
              onClick={() => setIsMobileMenuOpen(false)}
            >
              {item.label}
            </NavLink>
          ))}
          <div className="pt-3 mt-3 border-t border-green-800 flex flex-col gap-2">
            <Link
              to="/login"
              className="w-full text-center py-2.5 rounded-lg border border-green-600 text-sm font-medium hover:bg-zoo-green"
              onClick={() => setIsMobileMenuOpen(false)}
            >
              Đăng nhập
            </Link>
            <Link
              to="/register"
              className="w-full text-center py-2.5 rounded-lg bg-ticket-gold text-zoo-green-dark text-sm font-semibold hover:bg-amber-400"
              onClick={() => setIsMobileMenuOpen(false)}
            >
              Đăng ký tài khoản
            </Link>
          </div>
        </nav>
      )}
    </header>
  );
}