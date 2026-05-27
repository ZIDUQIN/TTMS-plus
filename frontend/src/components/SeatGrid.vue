<template>
  <div class="seat-grid-container">
    <!-- Screen indicator -->
    <div class="screen-indicator">
      <div class="screen-line"></div>
      <span>银 幕</span>
    </div>

    <!-- Row labels + Seat grid -->
    <div class="seats-area">
      <div v-for="row in rows" :key="row" class="seat-row">
        <span class="row-label">{{ rowLabel(row) }}</span>
        <div class="seat-cells">
          <div
            v-for="col in cols"
            :key="`${row}-${col}`"
            class="seat-cell"
            :class="getSeatClass(row, col)"
            @click="handleSeatClick(row, col)"
            :title="`${rowLabel(row)}-${String(col).padStart(2, '0')}`"
          >
            <el-icon v-if="getSeatClass(row, col) === 'occupied'" :size="14">
              <UserFilled />
            </el-icon>
            <el-icon v-else-if="getSeatClass(row, col) === 'locked'" :size="14">
              <Lock />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- Legend -->
    <div class="seat-legend">
      <div class="legend-item">
        <span class="legend-box available"></span>
        <span>可选</span>
      </div>
      <div class="legend-item">
        <span class="legend-box selected"></span>
        <span>已选</span>
      </div>
      <div class="legend-item">
        <span class="legend-box occupied"></span>
        <span>已售</span>
      </div>
      <div class="legend-item">
        <span class="legend-box locked"></span>
        <span>锁定</span>
      </div>
      <div class="legend-item">
        <span class="legend-box aisle"></span>
        <span>过道</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { UserFilled, Lock } from '@element-plus/icons-vue'

const props = defineProps({
  rowCount: {
    type: Number,
    required: true
  },
  colCount: {
    type: Number,
    required: true
  },
  seatStatusMap: {
    type: Object,
    default: () => ({})
  },
  selectedSeats: {
    type: Array,
    default: () => []
  },
  maxSelect: {
    type: Number,
    default: 6
  }
})

const emit = defineEmits(['select-seat', 'deselect-seat'])

const rows = computed(() => {
  const result = []
  for (let i = 1; i <= props.rowCount; i++) {
    result.push(i)
  }
  return result
})

const cols = computed(() => {
  const result = []
  for (let i = 1; i <= props.colCount; i++) {
    result.push(i)
  }
  return result
})

function rowLabel(row) {
  return String.fromCharCode(64 + row) // A, B, C...
}

function getSeatKey(row, col) {
  return `${rowLabel(row)}-${String(col).padStart(2, '0')}`
}

function getSeatClass(row, col) {
  const key = getSeatKey(row, col)
  // Check if selected
  if (props.selectedSeats.includes(key)) return 'selected'
  // Check seat map status
  const status = props.seatStatusMap[key]
  if (status === 'OCCUPIED' || status === 'SOLD' || status === 'sold') return 'occupied'
  if (status === 'LOCKED' || status === 'locked') return 'locked'
  if (status === 'AISLE' || status === 'aisle') return 'aisle'
  return 'available'
}

function handleSeatClick(row, col) {
  const key = getSeatKey(row, col)
  const cls = getSeatClass(row, col)

  // Cannot click occupied, locked, or aisle seats
  if (cls === 'occupied' || cls === 'locked' || cls === 'aisle') return

  if (cls === 'selected') {
    emit('deselect-seat', key)
  } else {
    if (props.selectedSeats.length >= props.maxSelect) {
      return
    }
    emit('select-seat', key)
  }
}
</script>

<style scoped>
.seat-grid-container {
  padding: 16px;
  user-select: none;
}

.screen-indicator {
  text-align: center;
  margin-bottom: 32px;
  filter: drop-shadow(0 0 8px rgba(255, 255, 255, 0.3));
}

.screen-line {
  height: 4px;
  background: linear-gradient(90deg, transparent, #ccc, transparent);
  border-radius: 50%;
  margin-bottom: 6px;
}

.screen-indicator span {
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 12px;
}

.seats-area {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}

.seat-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.row-label {
  width: 20px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  flex-shrink: 0;
}

.seat-cells {
  display: flex;
  gap: 6px;
}

.seat-cell {
  width: 28px;
  height: 28px;
  border-radius: 6px 6px 3px 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 10px;
}

.seat-cell.available {
  background: #67c23a;
  border: 1px solid #5daf34;
}

.seat-cell.available:hover {
  background: #85ce61;
  transform: scale(1.1);
}

.seat-cell.selected {
  background: #e6a23c;
  border: 1px solid #d49430;
  transform: scale(1.08);
}

.seat-cell.occupied {
  background: #f56c6c;
  border: 1px solid #e05555;
  cursor: not-allowed;
}

.seat-cell.locked {
  background: #909399;
  border: 1px solid #808389;
  cursor: not-allowed;
}

.seat-cell.aisle {
  background: transparent;
  border: none;
  cursor: default;
}

.seat-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-box {
  width: 16px;
  height: 16px;
  border-radius: 4px 4px 2px 2px;
}

.legend-box.available {
  background: #67c23a;
  border: 1px solid #5daf34;
}

.legend-box.selected {
  background: #e6a23c;
  border: 1px solid #d49430;
}

.legend-box.occupied {
  background: #f56c6c;
  border: 1px solid #e05555;
}

.legend-box.locked {
  background: #909399;
  border: 1px solid #808389;
}

.legend-box.aisle {
  background: transparent;
  border: 1px dashed var(--border-color);
}
</style>
