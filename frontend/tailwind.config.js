/** @type {import('tailwindcss').Config} */

/* Semantic colors resolve to the tokens in src/styles/design-tokens.css. */
const token = (name) => `hsl(var(--${name}) / <alpha-value>)`

module.exports = {
  darkMode: ['class', 'class'],
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
  	extend: {
  		colors: {
        background:          token('background'),
        foreground:          token('foreground'),

        card:                token('card'),
        'card-foreground':   token('card-foreground'),

        popover:             token('popover'),
        'popover-foreground':token('popover-foreground'),

        primary: {
          DEFAULT:           token('primary'),
          foreground:        token('primary-foreground'),
          hover:             token('primary-hover'),
          soft:              token('primary-soft'),
        },

        secondary:           token('secondary'),
        'secondary-foreground':
                              token('secondary-foreground'),

        muted:               token('muted'),
        'muted-foreground':  token('muted-foreground'),

        accent:              token('accent'),
        'accent-foreground': token('accent-foreground'),

        destructive:         token('destructive'),
        'destructive-foreground':
                              token('destructive-foreground'),

        success: {
          DEFAULT:           token('success'),
          foreground:        token('success-foreground'),
          soft:              token('success-soft'),
        },
        warning: {
          DEFAULT:           token('warning'),
          foreground:        token('warning-foreground'),
          soft:              token('warning-soft'),
        },
        info: {
          DEFAULT:           token('info'),
          foreground:        token('info-foreground'),
          soft:              token('info-soft'),
        },

        learning: {
          green:             token('learning-green'),
          yellow:            token('learning-yellow'),
          coral:             token('learning-coral'),
          blue:              token('learning-blue'),
        },

        role: {
          accent:            token('role-accent'),
          'accent-soft':     token('role-accent-soft'),
        },

        border:              token('border'),
        input: {
          DEFAULT:           token('input'),
          background:        token('input-background'),
        },
        ring:                token('ring'),
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
  		},
  		borderRadius: {
  			xl: 'calc(var(--radius) + 4px)',
  			lg: 'var(--radius)',
  			md: 'calc(var(--radius) - 2px)',
  			sm: 'calc(var(--radius) - 4px)'
  		},
  		boxShadow: {
  			xs: 'var(--shadow-xs)',
  			sm: 'var(--shadow-sm)',
  			md: 'var(--shadow-md)',
  			overlay: 'var(--shadow-overlay)'
  		}
  	}
  },
  plugins: [
    require('tailwindcss-animate'),
  ],
}
