import { IconSun, IconMoon } from '@tabler/icons-react'

export default function ThemeToggle({ theme, onToggle }) {
  return (
    <div
      style={{
        display: 'flex',
        width: 'fit-content',
        background: 'var(--color-bg-page)',
        borderRadius: '20px',
        padding: '3px',
        border: '0.5px solid var(--color-border)',
        gap: '2px',
      }}
    >
      {[
        { value: 'light', icon: IconSun, label: 'Light' },
        { value: 'dark', icon: IconMoon, label: 'Dark' },
      ].map(({ value, icon: Icon, label }) => (
        <button
          key={value}
          onClick={() => onToggle(value)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '5px',
            padding: '4px 12px',
            borderRadius: '20px',
            fontSize: '12px',
            cursor: 'pointer',
            border: theme === value ? '0.5px solid var(--color-border)' : 'none',
            background: theme === value ? 'var(--color-bg-sidebar)' : 'transparent',
            color: theme === value ? 'var(--color-text-primary)' : 'var(--color-text-secondary)',
            fontWeight: theme === value ? '500' : '400',
          }}
        >
          <Icon size={13} />
          {label}
        </button>
      ))}
    </div>
  )
}
