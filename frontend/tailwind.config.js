/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts,scss}",
  ],
  theme: {
    extend: {
      borderRadius: {
        'none': '0',
        'sm': '0',
        DEFAULT: '0',
        'md': '0',
        'lg': '0',
        'xl': '0',
        '2xl': '0',
        '3xl': '0',
        'full': '0',
      },
      colors: {
        sage: {
          DEFAULT: '#4B584E',
          dark: '#38433B',
          light: '#F4F5F4'
        }
      }
    },
  },
  plugins: [],
}
