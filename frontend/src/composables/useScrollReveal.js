/**
 * useScrollReveal — Apple-style viewport entrance animation
 *
 * Usage:
 *   <div v-scroll-reveal class="my-element">Content</div>
 *
 * Elements animate in: translateY(8px) + opacity 0 → translateY(0) + opacity 1
 * Animation fires once per element, 300ms ease-out, 8px upward drift.
 */
import { onMounted, onUnmounted, ref } from 'vue'

const observer = ref(null)
const observedElements = new Map()

function createObserver() {
  if (observer.value) return observer.value

  observer.value = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          // Unobserve after first reveal
          observer.value.unobserve(entry.target)
          observedElements.delete(entry.target)
        }
      }
    },
    {
      threshold: 0.15,
      rootMargin: '0px 0px -20px 0px'
    }
  )

  return observer.value
}

// Vue directive
export const vScrollReveal = {
  mounted(el) {
    el.classList.add('animate-enter')
    const obs = createObserver()
    obs.observe(el)
    observedElements.set(el, obs)
  },
  unmounted(el) {
    const obs = observedElements.get(el)
    if (obs) {
      obs.unobserve(el)
      observedElements.delete(el)
    }
  }
}

// Composable for manual use
export function useScrollReveal() {
  onMounted(() => {
    createObserver()
  })

  onUnmounted(() => {
    // Cleanup handled per-element via directive
  })

  return {
    vScrollReveal
  }
}
