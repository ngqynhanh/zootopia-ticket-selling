/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'zoo-green': '#2d6a4f',
        'zoo-green-dark': '#1b4332',
        'wood-brown': '#7f4f24',
        'ticket-gold': '#f5b041',
      },
    },
  },
  plugins: [],
}