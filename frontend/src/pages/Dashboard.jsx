import { useOpenPullRequests } from '../api/pullRequests'
import RepoSection from '../components/pr/RepoSection.jsx'
import PageHeader from '../components/ui/PageHeader.jsx'

function groupByRepo(prs) {
  return prs.reduce((acc, pr) => {
    if (!acc[pr.repo]) acc[pr.repo] = []
    acc[pr.repo].push(pr)
    return acc
  }, {})
}

export default function Dashboard() {
  const { data: prs, isLoading, isError } = useOpenPullRequests()

  if (isLoading) {
    return (
      <div style={{ padding: '2rem', color: 'var(--color-text-secondary)', fontSize: '14px' }}>
        Loading...
      </div>
    )
  }

  if (isError) {
    return (
      <div style={{ padding: '2rem', color: 'var(--color-fail-text)', fontSize: '14px' }}>
        Something went wrong loading pull requests.
      </div>
    )
  }

  const grouped = groupByRepo(prs)
  const repoCount = Object.keys(grouped).length
  const prCount = prs.length

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <PageHeader
        title="Dashboard"
        subtitle={`${prCount} open ${prCount === 1 ? 'pull request' : 'pull requests'} across ${repoCount} ${repoCount === 1 ? 'repo' : 'repos'}`}
      />
      <div style={{ padding: '1.5rem' }}>
        {prCount === 0 ? (
          <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>
            No open pull requests.
          </p>
        ) : (
          Object.entries(grouped).map(([repo, repoPrs]) => (
            <RepoSection key={repo} repo={repo} prs={repoPrs} />
          ))
        )}
      </div>
    </div>
  )
}