/** @type {import('tailwindcss').Config} */

module.exports = {
  darkMode: ['class', 'class'],
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
  	extend: {
  		colors: {
        /* --- primary / secondary / accent / etc. --- */
        primary:             'hsl(var(--primary) / <alpha-value>)',
        'primary-foreground':'hsl(var(--primary-foreground) / <alpha-value>)',

        secondary:           'hsl(var(--secondary) / <alpha-value>)',
        'secondary-foreground':
                              'hsl(var(--secondary-foreground) / <alpha-value>)',

        accent:              'hsl(var(--accent) / <alpha-value>)',
        'accent-foreground': 'hsl(var(--accent-foreground) / <alpha-value>)',

        destructive:         'hsl(var(--destructive) / <alpha-value>)',
        'destructive-foreground':
                              'hsl(var(--destructive-foreground) / <alpha-value>)',

        muted:               'hsl(var(--muted) / <alpha-value>)',
        'muted-foreground':  'hsl(var(--muted-foreground) / <alpha-value>)',

        border:              'hsl(var(--border) / <alpha-value>)',
        input:               'hsl(var(--input) / <alpha-value>)',
        ring:                'hsl(var(--ring) / <alpha-value>)',
        card:                'hsl(var(--card) / <alpha-value>)',
        'card-foreground':   'hsl(var(--card-foreground) / <alpha-value>)',
  			sidebar: {
  				DEFAULT: 'hsl(var(--sidebar-background))',
  				foreground: 'hsl(var(--sidebar-foreground))',
  				primary: 'hsl(var(--sidebar-primary))',
  				'primary-foreground': 'hsl(var(--sidebar-primary-foreground))',
  				accent: 'hsl(var(--sidebar-accent))',
  				'accent-foreground': 'hsl(var(--sidebar-accent-foreground))',
  				border: 'hsl(var(--sidebar-border))',
  				ring: 'hsl(var(--sidebar-ring))'
  			}
  		}
  	}
  },
  plugins: [
    require('tailwindcss-animate'),
  ],
}
