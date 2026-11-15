import { useState, useCallback } from 'react'

/**
 * Generic async data-fetching hook.
 * Returns { data, loading, error, execute }.
 */
export function useApi(apiFn, options = {}) {
  const { immediate = false, initialData = null } = options
  const [data, setData]       = useState(initialData)
  const [loading, setLoading] = useState(immediate)
  const [error, setError]     = useState(null)

  const execute = useCallback(async (...args) => {
    setLoading(true)
    setError(null)
    try {
      const res = await apiFn(...args)
      const result = res?.data ?? res
      setData(result)
      return result
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'An error occurred'
      setError(msg)
      throw err
    } finally {
      setLoading(false)
    }
  }, [apiFn])

  return { data, loading, error, execute, setData }
}
