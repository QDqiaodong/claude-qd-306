/**
 * 一个很轻的事件总线：列表页改完数据，别的页面（比如顶部的计数）也能跟着刷新。
 * 不用第三方库，十几行就够。
 */
const handlers = new Map()

export function on(event, fn) {
  if (!handlers.has(event)) handlers.set(event, new Set())
  handlers.get(event).add(fn)
  return () => handlers.get(event).delete(fn)
}

export function emit(event, payload) {
  const set = handlers.get(event)
  if (set) set.forEach((fn) => fn(payload))
}
