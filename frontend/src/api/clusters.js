import { useQuery } from '@tanstack/react-query'

async function fetchClusters() {
  const res = await fetch('/api/clusters')
  if (!res.ok) throw new Error(`Failed to fetch clusters: ${res.status}`)
  return res.json()
}

async function fetchCluster(id) {
  const res = await fetch(`/api/clusters/${id}`)
  if (!res.ok) throw new Error(`Failed to fetch cluster: ${res.status}`)
  return res.json()
}

export function useClusters() {
  return useQuery({
    queryKey: ['clusters'],
    queryFn: fetchClusters,
  })
}

export function useCluster(id) {
  return useQuery({
    queryKey: ['clusters', id],
    queryFn: () => fetchCluster(id),
  })
}
