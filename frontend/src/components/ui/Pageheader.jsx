export default function PageHeader({ title, subtitle }) {
  return (
    <div
    style={{
        padding: '0 1.5rem',
        background: 'var(--color-bg-sidebar)',
        borderBottom: '0.5px solid var(--color-border)',
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        height: '72px',
    }}
    >
      <div
        style={{
          width: '4px',
          height: '40px',
          background: 'var(--color-accent)',
          borderRadius: '2px',
          flexShrink: 0,
        }}
      />
      <div>
        <h1 style={{ fontSize: '20px', fontWeight: '500', margin: '0 0 3px', color: 'var(--color-text-primary)' }}>
        {title}
        </h1>
        {subtitle && (
        <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', margin: 0 }}>
            {subtitle}
        </p>
        )}
      </div>
    </div>
  )
}