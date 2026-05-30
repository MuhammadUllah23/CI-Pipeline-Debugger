export default function Tabs({ tabs, activeTab, onTabChange }) {
  return (
    <div
      style={{
        display: 'flex',
        borderBottom: '0.5px solid var(--color-border)',
        marginBottom: '1.5rem',
      }}
    >
      {tabs.map(tab => (
        <button
          key={tab}
          onClick={() => onTabChange(tab)}
          style={{
            padding: '8px 16px',
            fontSize: '13px',
            cursor: 'pointer',
            background: 'transparent',
            border: 'none',
            borderBottom: activeTab === tab ? '2px solid var(--color-accent)' : '2px solid transparent',
            color: activeTab === tab ? 'var(--color-accent)' : 'var(--color-text-secondary)',
            fontWeight: activeTab === tab ? '500' : '400',
            marginBottom: '-0.5px',
          }}
        >
          {tab}
        </button>
      ))}
    </div>
  )
}