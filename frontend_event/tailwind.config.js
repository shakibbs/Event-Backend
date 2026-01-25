
/** @type {import('tailwindcss').Config} */
export default {
  content: [
  './index.html',
  './src/**/*.{js,ts,jsx,tsx}'
],
  theme: {
    extend: {
      colors: {
        // Existing Enterprise Theme
        primary: {
          DEFAULT: '#1e293b', // Slate 800
          light: '#334155', // Slate 700
          dark: '#0f172a', // Slate 900
        },
        accent: {
          DEFAULT: '#3b82f6', // Blue 500
          light: '#60a5fa', // Blue 400
          dark: '#2563eb', // Blue 600
        },
        surface: {
          DEFAULT: '#ffffff',
          secondary: '#f8fafc', // Slate 50
          tertiary: '#f1f5f9', // Slate 100
        },
        text: {
          primary: '#0f172a', // Slate 900
          secondary: '#64748b', // Slate 500
          tertiary: '#94a3b8', // Slate 400
        },
        // New Public Theme (Template 4)
        coral: {
          DEFAULT: '#ff6b6b',
          light: '#ff8787',
          dark: '#fa5252',
        },
        cream: {
          DEFAULT: '#fef6e4',
          dark: '#faeec7',
        },
        warm: {
          gray: '#f9f8f6',
          text: '#2d2a26',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['"Playfair Display"', 'serif'],
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-out',
        'slide-up': 'slideUp 0.5s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(20px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
      },
    },
  },
  plugins: [],
}
