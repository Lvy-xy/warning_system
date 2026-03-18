<template>
  <div class="strategy-config-container">
    <div class="left-panel">
      <div class="panel-header">
        <span>农区及作物分布</span>
      </div>
      <el-input v-model="filterText" placeholder=" 搜索农区 / 作物 ..." prefix-icon="Search" clearable
class="tree-search" />
      <el-tree
        ref="treeRef"
        class="farm-tree"
        :data="treeData"
        :props="defaultProps"
        :filter-node-method="filterNode"
        default-expand-all
        highlight-current
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <el-icon v-if="data.type === 'zone'"><Location /></el-icon>
            <el-icon v-else-if="data.type === 'greenhouse'"><House /></el-icon>
            <el-icon v-else style="color: #67C23A"><Crop /></el-icon>
            <span style="margin-left: 5px;">{{ node.label }}</span>
          </span>
        </template>
      </el-tree>
    </div>
    <div class="right-panel">
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="title"> 水 肥 闭 环 联 动 策 略 配 置  - <el-tag
type="success">{{ currentTarget }}</el-tag></span>
            <div class="action-btns">
              <el-switch
                v-model="strategyStatus"
                inline-prompt
                active-text="策略已启"
                inactive-text="策略已停"
                style="margin-right: 15px;"
                @change="handleStatusChange"
              />
              <el-button type="primary" icon="Upload" @click="handleIssueStrategy"> 下 发 策 略
</el-button>
            </div>
          </div>
        </template>
        <el-form :model="formData" label-width="130px" class="strategy-form">
          <el-divider content-position="left">触发条件设定 (Trigger Conditions)</el-divider>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="核心监测指标">
                <el-select v-model="formData.triggerMetric" placeholder="请选择">
                  <el-option label="平均土壤含水率" value="moisture" />
                  <el-option label="土壤基质 EC 值" value="ec" />
                  <el-option label="叶面温度" value="temp" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="触发阈值">
                <el-input v-model="formData.triggerThreshold" placeholder="请输入">
                  <template #prepend>低于</template>
                  <template #append>%</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="持续时间">
                <el-input-number v-model="formData.triggerDuration" :min="0.5" :step="0.5" />
                <span class="unit-text">小时</span>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="辅助气象条件">
                <el-checkbox-group v-model="formData.weatherConditions">
                  <el-checkbox label="无降雨预警" />
                  <el-checkbox label="光照强度>10000Lx" />
                  <el-checkbox label="风速<5m/s" />
                </el-checkbox-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-divider content-position="left">动作参数设定 (Action Parameters)</el-divider>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="水肥配方比例">
                <el-input v-model="formData.formulaRatio" placeholder="N:P:K" />
                <div class="sub-text">格式: 氮(N)-磷(P)-钾(K)</div>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="目标 EC 值">
                <el-input-number v-model="formData.targetEC" :min="1.0" :max="5.0" :step="0.1" />
                <span class="unit-text">mS/cm</span>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="目标 PH 值">
                <el-input-number v-model="formData.targetPH" :min="4.0" :max="8.0" :step="0.1" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="单次灌溉水量">
                <el-input-number v-model="formData.irrigationVolume" :min="1" />
                <span class="unit-text">m³ / 亩</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="灌溉持续时长">
                <el-input-number v-model="formData.irrigationDuration" :min="10" :step="10" />
                <span class="unit-text">分钟</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="管网冲洗时长">
                <el-input-number v-model="formData.flushDuration" :min="1" :step="1" />
                <span class="unit-text">分钟</span>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-card>
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="title"> 当前策略下预期周水肥消耗测试对比  (数据预测周期 : 2025 -06-16 至
2025-06-22)</span>
            <el-tooltip content="基于历史同期气象与长势数据聚合计算" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
        </template>
        <div ref="chartRef" class="echarts-container"></div>
      </el-card>
    </div>
    <el-dialog v-model="dialogVisible" title="策略下发设备确认" width="800px" destroy-on-close>
      <div class="dialog-tip">
        <el-icon color="#E6A23C"><WarningFilled /></el-icon>
        <span>系统即将向以下边缘智控终端下发最新《{{ currentTarget }} - 自动水肥联动策略》，请确认
设备状态。</span>
      </div>
      <el-table :data="deviceTableData" border stripe style="width: 100%; margin-top: 15px;">
        <el-table-column prop="deviceId" label="终端编号" width="150" />
        <el-table-column prop="deviceName" label="受控设备" width="180" />

        <el-table-column prop="area" label="控制区域" />
        <el-table-column label="网络状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '在线' ? 'success' : 'danger'" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSync" label="最后同步时间 (2025)" width="180" />
      </el-table>
      <div class="dialog-summary">
        预计耗水：<span class="highlight">{{ Math.round(formData.irrigationVolume * 15) }} m³</span> |
        预计耗肥：<span class="highlight">{{ estimatedFertilizer }} kg</span>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="confirmIssue" :loading="issuing"> 确  认  下  发
</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, watch, onMounted, nextTick } from 'vue'
import { Search, Location, House, Crop, InfoFilled, WarningFilled, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'
const filterText = ref('')
const treeRef = ref(null)
const currentTarget = ref('A1区-普罗旺斯番茄')
const treeData = ref([])
const defaultProps = { children: 'children', label: 'label' }
watch(filterText, (val) => {
  if (treeRef.value && treeRef.value.filter) treeRef.value.filter(val)
})
const filterNode = (value, data) => {
  if (!value) return true
  return data.label && data.label.includes(value)
}
const strategyStatus = ref(true)
const formData = reactive({
  triggerMetric: 'moisture',
  triggerThreshold: '45',
  triggerDuration: 2.0,
  weatherConditions: ['无降雨预警', '光照强度>10000Lx'],
  formulaRatio: '15-5-25',
  targetEC: 2.2,

  targetPH: 6.0,
  irrigationVolume: 8,
  irrigationDuration: 45,
  flushDuration: 5
})
const deviceTableData = ref([])
const dialogVisible = ref(false)
const issuing = ref(false)
const chartRef = ref(null)
let myChart = null
const estimatedFertilizer = ref(24.5)
const initChart = () => {
  if (!chartRef.value) return
  myChart = echarts.init(chartRef.value)
  const defaultDates = ['2025 -06-16', '2025 -06-17', '2025 -06-18', '2025 -06-19', '2025 -06-20', '2025 -06-21',
'2025-06-22']
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['预期水消耗(m³)', '历史均值-水', '预期肥消耗(kg)', '历史均值-肥'],
      bottom: 0
    },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '15%', containLabel: true },
    xAxis: [
      {
        type: 'category',
        data: defaultDates,
        axisPointer: { type: 'shadow' }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '水量 (m³)',
        min: 0,
        max: 50,
        interval: 10,
        axisLabel: { formatter: '{value}' }
      },
      {
        type: 'value',
        name: '肥料 (kg)',
        min: 0,
        max: 20,
        interval: 4,
        axisLabel: { formatter: '{value}' }
      }

    ],
    series: [
      {
        name: '预期水消耗(m³)',
        type: 'bar',
        barWidth: '15%',
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
        data: [28, 30, 25, 0, 35, 32, 28]
      },
      {
        name: '历史均值-水',
        type: 'line',
        itemStyle: { color: '#79bbff' },
        lineStyle: { type: 'dashed', width: 2 },
        data: [30, 32, 28, 10, 38, 35, 30]
      },
      {
        name: '预期肥消耗(kg)',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: '15%',
        itemStyle: { color: '#E6A23C', borderRadius: [4, 4, 0, 0] },
        data: [12, 14, 10, 0, 16, 15, 12]
      },
      {
        name: '历史均值-肥',
        type: 'line',
        yAxisIndex: 1,
        itemStyle: { color: '#F3D19E' },
        lineStyle: { type: 'dashed', width: 2 },
        data: [15, 16, 13, 5, 18, 17, 14]
      }
    ]
  }
  myChart.setOption(option)
}
const setChartData = (payload) => {
  if (!myChart) return
  const dates = payload.dates || ['2025 -06-16', '2025 -06-17', '2025 -06-18', '2025 -06-19', '2025 -06-20',
'2025-06-21', '2025-06-22']
  const waterForecast = payload.waterForecast || [28, 30, 25, 0, 35, 32, 28]
  const waterHistory = payload.waterHistory || [30, 32, 28, 10, 38, 35, 30]
  const fertForecast = payload.fertForecast || [12, 14, 10, 0, 16, 15, 12]
  const fertHistory = payload.fertHistory || [15, 16, 13, 5, 18, 17, 14]
  myChart.setOption({
    xAxis: [{ data: dates }],
    series: [
      { data: waterForecast },
      { data: waterHistory },
      { data: fertForecast },

      { data: fertHistory }
    ]
  })
}
const loadTreeData = async () => {
  try {
    const res = await request({
      url: '/farm/tree',
      method: 'get'
    })
    treeData.value = res && res.data && res.data.length ? res.data : [
      {
        id: 1,
        label: '东部高标农田示范区',
        type: 'zone',
        children: [
          {
            id: 11,
            label: 'A1 区 (连栋温室)',
            type: 'greenhouse',
            children: [{ id: 111, label: '普罗旺斯番茄', type: 'crop' }]
          },
          {
            id: 12,
            label: 'A2 区 (日光温室)',
            type: 'greenhouse',
            children: [{ id: 121, label: '荷兰水果黄瓜', type: 'crop' }]
          }
        ]
      },
      {
        id: 2,
        label: '南部露天果园基地',
        type: 'zone',
        children: [
          {
            id: 21,
            label: 'B1 区 (网室)',
            type: 'greenhouse',
            children: [{ id: 211, label: '红颜草莓', type: 'crop' }]
          }
        ]
      }
    ]
  } catch (e) {
    treeData.value = [
      {
        id: 1,
        label: '东部高标农田示范区',
        type: 'zone',

        children: [
          {
            id: 11,
            label: 'A1 区 (连栋温室)',
            type: 'greenhouse',
            children: [{ id: 111, label: '普罗旺斯番茄', type: 'crop' }]
          }
        ]
      }
    ]
  }
}
const loadStrategyAndDevices = async (target) => {
  try {
    const res = await request({
      url: '/strategy/get',
      method: 'get',
      params: { target }
    })
    if (res && res.data) {
      if (typeof res.data.strategyStatus === 'boolean') strategyStatus.value = res.data.strategyStatus
      if (res.data.formData) {
        Object.keys(formData).forEach(key => {
          if (res.data.formData[key] !== undefined) formData[key] = res.data.formData[key]
        })
      }
      if (res.data.estimatedFertilizer !== undefined) estimatedFertilizer.value = res.data.estimatedFertilizer
    }
  } catch (e) {}
  try {
    const devRes = await request({
      url: '/devices/list',
      method: 'get',
      params: { target }
    })
    deviceTableData.value = devRes && devRes.data && devRes.data.length ? devRes.data : [
      { deviceId: 'CTRL -A1-001', deviceName: ' 施肥机 A 主泵控制器', area: 'A1 区', status: ' 在线', lastSync:
'2025-06-15 14:32:11' },
      { deviceId: 'VALVE-A1-102', deviceName: '滴灌电磁阀组-01', area: 'A1 区(1-5 跨)', status: '在线', lastSync:
'2025-06-15 14:33:05' },
      { deviceId: 'VALVE -A1-103', deviceName: ' 滴灌电磁阀组-02', area: 'A1 区(6-10 跨)', status: ' 在线',
lastSync: '2025-06-15 14:31:50' },
      { deviceId: 'SENS -A1-S01', deviceName: ' 多深度土壤测控仪', area: 'A1 区中心', status: ' 在线', lastSync:
'2025-06-15 14:33:12' }
    ]
  } catch (e) {}
}
const loadChart = async (target) => {
  try {
    const res = await request({

      url: '/forecast/chart',
      method: 'get',
      params: { target }
    })
    if (res && res.data) {
      setChartData(res.data)
    } else {
      setChartData({})
    }
  } catch (e) {
    setChartData({})
  }
}
const handleNodeClick = (data) => {
  if (data.type === 'crop') {
    const node = treeRef.value && treeRef.value.getNode ? treeRef.value.getNode(data) : null
    const parentLabel = node && node.parent && node.parent.data ? node.parent.data.label : ''
    currentTarget.value = parentLabel ? `${parentLabel}-${data.label}` : data.label
    loadStrategyAndDevices(currentTarget.value)
    loadChart(currentTarget.value)
  }
}
const handleStatusChange = async (val) => {
  try {
    const res = await request({
      url: '/strategy/updateStatus',
      method: 'post',
      data: { target: currentTarget.value, status: val }
    })
    if (res && res.data && res.data.success) {
      ElMessage({
        message: val ? '自动控制策略已启动，系统将实时监听预警阈值。' : '自动控制策略已挂起。',
        type: val ? 'success' : 'warning'
      })
    } else {
      strategyStatus.value = !val
      ElMessage({ message: '更新策略状态失败，请重试。', type: 'error' })
    }
  } catch (e) {
    strategyStatus.value = !val
    ElMessage({ message: '更新策略状态失败，请重试。', type: 'error' })
  }
}
const handleIssueStrategy = () => {
  dialogVisible.value = true
}
const confirmIssue = async () => {
  issuing.value = true
  try {
    const payload = { target: currentTarget.value, formData: JSON.parse(JSON.stringify(formData)) }

    const res = await request({
      url: '/strategy/issue',
      method: 'post',
      data: payload
    })
    issuing.value = false
    dialogVisible.value = false
    const jobId = res && res.data && res.data.jobId ? res.data.jobId : `${new Date().getTime()}`
    ElNotification({
      title: '指令下发成功',
      message: `策略已成功同步至底层智控网关。流水号: ${jobId}`,
      type: 'success',
      duration: 3000
    })
  } catch (e) {
    issuing.value = false
    ElMessage({ message: '下发策略失败，请重试。', type: 'error' })
  }
}
onMounted(() => {
  nextTick(async () => {
    await loadTreeData()
    await loadStrategyAndDevices(currentTarget.value)
    initChart()
    await loadChart(currentTarget.value)
    window.addEventListener('resize', () => {
      myChart && myChart.resize()
    })
  })
})
</script>
<style scoped>
.strategy-config-container {
  display: flex;
  height: calc(100vh - 40px);
  background-color: #f0f2f5;
  padding: 20px;
  box-sizing: border-box;
  gap: 20px;
}
.left-panel {
  width: 280px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-header {

  padding: 15px 20px;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}
.tree-search {
  margin: 15px;
  width: calc(100% - 30px);
}
.farm-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px;
}
.custom-tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}
.form-card {
  border-radius: 8px;
  flex: none;
}
.chart-card {
  border-radius: 8px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
:deep(.chart-card .el-card__body) {
  flex: 1;
  padding: 10px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header .title {
  font-size: 16px;
  font-weight: 600;
  display: flex;

  align-items: center;
  gap: 10px;
}
.action-btns {
  display: flex;
  align-items: center;
}
.strategy-form {
  padding: 0 20px;
}
.unit-text {
  margin-left: 10px;
  color: #909399;
}
.sub-text {
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.2;
  margin-top: 5px;
  width: 100%;
}
.echarts-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
}
.info-icon {
  color: #909399;
  cursor: pointer;
}
.dialog-tip {
  display: flex;
  align-items: center;
  background-color: #fdf6ec;
  padding: 10px 15px;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 14px;
  gap: 8px;
}
.dialog-summary {
  margin-top: 15px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}
.dialog-summary .highlight {
  color: #409eff;
  font-weight: bold;
  font-size: 16px;

}
</style>

