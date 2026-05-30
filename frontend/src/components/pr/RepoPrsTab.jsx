import PrCard from './PrCard.jsx'
import Pagination from '../ui/Pagination.jsx'

const PR_STATUS = ['open', 'merged']

export default function RepoPrsTab({ prs, isLoading, status, page, totalPages, onStatusChange, onPrevious, onNext }) {
  return (
    <>
      <div
        style={{
          display: 'flex',
          background: 'var(--color-bg-page)',
          borderRadius: '20px',
          padding: '3px',
          border: '0.5px solid var(--color-border)',
          gap: '2px',
          width: 'fit-content',
          marginBottom: '1rem',
        }}
      >
        {PR_STATUS.map(s => (
          <button
            key={s}
            onClick={() => onStatusChange(s)}
            style={{
              padding: '4px 14px',
              borderRadius: '20px',
              fontSize: '12px',
              cursor: 'pointer',
              border: status === s ? '0.5px solid var(--color-border)' : 'none',
              background: status === s ? 'var(--color-bg-sidebar)' : 'transparent',
              color: status === s ? 'var(--color-text-primary)' : 'var(--color-text-secondary)',
              fontWeight: status === s ? '500' : '400',
            }}
          >
            {s.charAt(0).toUpperCase() + s.slice(1)}
          </button>
        ))}
      </div>

      {isLoading ? (
        <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>Loading...</p>
      ) : prs.length === 0 ? (
        <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>No {status} pull requests.</p>
      ) : (
        prs.map(pr => <PrCard key={pr.id} pr={pr} />)
      )}

      <Pagination
        page={page}
        hasMore={page < totalPages - 1}
        onPrevious={onPrevious}
        onNext={onNext}
      />
    </>
  )
}