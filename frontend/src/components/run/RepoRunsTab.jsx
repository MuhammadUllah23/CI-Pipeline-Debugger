import RunSet from 'RunSet.jsx'
import Pagination from '../ui/Pagination.jsx'

export default function RepoRunsTab({ sets, isLoading, page, totalPages, onPrevious, onNext }) {
  if (isLoading) {
    return <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>Loading...</p>
  }

  if (sets.length === 0) {
    return (
      <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>
        No main branch runs found.
      </p>
    )
  }

  return (
    <>
      {sets.map((set, index) => (
        <RunSet
          key={set.headSha}
          runs={set.runs}
          isLatest={page === 0 && index === 0}
          defaultExpanded={page === 0 && index === 0}
        />
      ))}
      <Pagination
        page={page}
        hasMore={page < totalPages - 1}
        onPrevious={onPrevious}
        onNext={onNext}
      />
    </>
  )
}
