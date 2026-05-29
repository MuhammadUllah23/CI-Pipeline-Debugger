export default function Pagination({ page, hasMore, onPrevious, onNext }) {
  if (page === 0 && !hasMore) return null

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '8px',
        marginTop: '1.5rem',
        paddingTop: '1rem',
        borderTop: '0.5px solid var(--color-border)',
      }}
    >
      {page > 0 && (
        <button
          onClick={onPrevious}
          style={{
            padding: '6px 14px',
            fontSize: '13px',
            color: 'var(--color-text-secondary)',
            background: 'transparent',
            border: '0.5px solid var(--color-border)',
            borderRadius: '8px',
            cursor: 'pointer',
          }}
        >
          ← Previous
        </button>
      )}
      <span style={{ fontSize: '13px', color: 'var(--color-text-secondary)', padding: '0 8px' }}>
        Page {page + 1}
      </span>
      {hasMore && (
        <button
          onClick={onNext}
          style={{
            padding: '6px 14px',
            fontSize: '13px',
            color: 'var(--color-text-secondary)',
            background: 'transparent',
            border: '0.5px solid var(--color-border)',
            borderRadius: '8px',
            cursor: 'pointer',
          }}
        >
          Next →
        </button>
      )}
    </div>
  )
}
