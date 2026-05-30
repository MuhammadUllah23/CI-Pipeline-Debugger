import { useQuery } from '@tanstack/react-query'

async function fetchRuns() {
  const res = await fetch('/api/runs')
  if (!res.ok) throw new Error(`Failed to fetch runs: ${res.status}`)
  return res.json()
}

async function fetchRun(id) {
  const res = await fetch(`/api/runs/${id}`)
  if (!res.ok) throw new Error(`Failed to fetch run: ${res.status}`)
  return res.json()
}

async function fetchRunSteps(id) {
  const res = await fetch(`/api/runs/${id}/steps`)
  if (!res.ok) throw new Error(`Failed to fetch run steps: ${res.status}`)
  return res.json()
}

async function fetchRunClusters(id) {
  const res = await fetch(`/api/runs/${id}/clusters`)
  if (!res.ok) throw new Error(`Failed to fetch run clusters: ${res.status}`)
  return res.json()
}

async function fetchRepos() {
  const res = await fetch('/api/repos')
  if (!res.ok) throw new Error(`Failed to fetch repos: ${res.status}`)
  return res.json()
}

async function fetchMainBranchRunSets(owner, repo, page) {
  const res = await fetch(`/api/runs/${owner}/${repo}/run-sets?page=${page}`)
  if (!res.ok) throw new Error(`Failed to fetch run sets: ${res.status}`)
  return res.json()
}

export function useRuns() {
  return useQuery({
    queryKey: ['runs'],
    queryFn: fetchRuns,
  })
}

export function useRun(id) {
  return useQuery({
    queryKey: ['runs', id],
    queryFn: () => fetchRun(id),
  })
}

export function useRunSteps(id) {
  return useQuery({
    queryKey: ['runs', id, 'steps'],
    queryFn: () => fetchRunSteps(id),
    refetchInterval: (query) => {
      const data = query.state.data
      if (!data || data.length === 0) return 3000
      const hasUnresolvedFailure = data.some(
        (step) => step.conclusion === 'FAILURE' && !step.errorSnippet
      )
      if (hasUnresolvedFailure) return 3000
      return false
    },
  })
}

export function useRunClusters(id, conclusion) {
  return useQuery({
    queryKey: ['runs', id, 'clusters'],
    queryFn: () => fetchRunClusters(id),
    refetchInterval: (query) => {
      const data = query.state.data
      if (conclusion === 'FAILURE' && (!data || data.length === 0)) return 5000
      return false
    },
  })
}

export function useRepos() {
  return useQuery({
    queryKey: ['repos'],
    queryFn: fetchRepos,
  })
}

export function useMainBranchRunSets(owner, repo, page) {
  return useQuery({
    queryKey: ['runs', owner, repo, 'run-sets', page],
    queryFn: () => fetchMainBranchRunSets(owner, repo, page),
    refetchInterval: 30000,
  })
}
