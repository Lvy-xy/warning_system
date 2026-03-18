<template>
  <div class="irrigation-console">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchQuery" class="search-form" @keyup.enter="handleSearch">
        <el-form-item label="动作类型">
          <el-select v-model="searchQuery.actionType" placeholder=" 全部 " clearable style="width:140px;">
            <el-option label="灌溉" value="irrigation" />
            <el-option label="施肥" value="fertilize" />
            <el-option label="开阀" value="open_valve" />
            <el-option label="关阀" value="close_valve" />
          </el-select>
        </el-form-item>

        <el-form-item label="执行状态">
          <el-select v-model="searchQuery.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="排队中" value="pending" />
            <el-option label="执行中" value="running" />
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编号">
          <el-input v-model="searchQuery.deviceId" placeholder="请输入设备编号" clearable />
        </el-form-item>
        <el-form-item label="所属农区">
          <el-input v-model="searchQuery.zoneName" placeholder="如: A 区" clearable />
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker
            v-model="searchQuery.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openAddDialog">下发控制指令</el-button>
        <div class="right-tools">
          <el-tag type="info">共 {{ total }} 条记录</el-tag>
        </div>
      </div>
      <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="actionType" label="动作类型" width="100" align="center">
          <template #default="{ row }">

<el-tag :type="getActionTagType(row.actionType)">{{ getActionName(row.actionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviceId" label=" 目标设备编号" min-width="140" show-overflow-tooltip
/>
        <el-table-column prop="zoneName" label="所属农区" min-width="120" show-overflow-tooltip />

        <el-table-column prop="scheduledAt" label="计划执行时间" width="160" align="center" />
        <el-table-column prop="executedAt" label="测试执行时间" width="160" align="center">
          <template #default="{ row }">
            {{ row.executedAt || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="触发方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.operator === 'system' ? 'primary' : 'warning'" effect="plain">
              {{ row.operator === 'system' ? '系统自动' : '手动干预' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试" width="70" align="center">
          <template #default="{ row }">
            <span :class="{ 'error-text': row.retryCount > 0 }">{{ row.retryCount }} 次</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="执行状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)"
effect="dark">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'running'"
              type="danger"
              link
              :icon="VideoPause"
              @click="handleTerminate(row)">
              强行终止
            </el-button>
            <el-button
              v-if="row.status === 'failed'"
              type="primary"
              link
              :icon="RefreshRight"
              @click="handleRetry(row)">
              一键重试
            </el-button>
            <el-button type="info" link :icon="Edit"
@click="openEditDialog(row)" :disabled="['running'].includes(row.status)">编辑</el-button>
            <el-popconfirm title="确定要删除此记录吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>

        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    <el-dialog :title="dialogType === 'add' ? ' 下发控制指令' : ' 编辑控制指令'" v-model="dialogVisible"
width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="动作类型" prop="actionType">
          <el-select v-model="form.actionType" placeholder="请选择控制动作" style="width: 100%;">
            <el-option label="灌溉" value="irrigation" />
            <el-option label="施肥" value="fertilize" />
            <el-option label="开阀" value="open_valve" />
            <el-option label="关阀" value="close_valve" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标设备" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="所属农区" prop="zoneName">
          <el-input v-model="form.zoneName" placeholder="请输入所属农区" />
        </el-form-item>
        <el-form-item label="计划时间" prop="scheduledAt">
          <el-date-picker
            v-model="form.scheduledAt"
            type="datetime"
            placeholder="选择计划执行时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="执行状态" prop="status" v-if="dialogType === 'edit'">
          <el-select v-model="form.status" style="width: 100%;">
            <el-option label="排队中" value="pending" />
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Plus, Edit, Delete, VideoPause, RefreshRight
} from '@element-plus/icons-vue'
import request from '@/utils/request'
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const searchQuery = reactive({
  actionType: '',
  status: '',
  deviceId: '',
  zoneName: '',
  dateRange: null
})
const pagination = reactive({
  currentPage: 1,
  pageSize: 10
})
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const form = reactive({
  id: null,
  actionType: '',
  deviceId: '',
  zoneName: '',
  scheduledAt: '',
  status: 'pending'
})
const rules = {
  actionType: [{ required: true, message: '请选择动作类型', trigger: 'change' }],
  deviceId: [{ required: true, message: '请输入目标设备编号', trigger: 'blur' }],
  zoneName: [{ required: true, message: '请输入所属农区', trigger: 'blur' }],
  scheduledAt: [{ required: true, message: '请选择计划执行时间', trigger: 'change' }]
}
const getActionName = (type) => {
  const map = { irrigation: '灌溉', fertilize: '施肥', open_valve: '开阀', close_valve: '关阀' }
  return map[type] || type

}
const getActionTagType = (type) => {
  const map = { irrigation: 'primary', fertilize: 'success', open_valve: 'warning', close_valve: 'danger' }
  return map[type] || 'info'
}
const getStatusName = (status) => {
  const map = { pending: '排队中', running: '执行中', success: '成功', failed: '失败', cancelled: '已取消' }
  return map[status] || status
}
const getStatusTagType = (status) => {
  const map = { pending: 'info', running: '', success: 'success', failed: 'danger', cancelled: 'info' }
  return map[status] || 'info'
}
const buildListParams = () => {
  const params = {
    page: pagination.currentPage,
    pageSize: pagination.pageSize
  }
  if (searchQuery.actionType) params.actionType = searchQuery.actionType
  if (searchQuery.status) params.status = searchQuery.status
  if (searchQuery.deviceId) params.deviceId = searchQuery.deviceId
  if (searchQuery.zoneName) params.zoneName = searchQuery.zoneName
  if (searchQuery.dateRange && searchQuery.dateRange.length === 2) {
    params.startAt = searchQuery.dateRange[0]
    params.endAt = searchQuery.dateRange[1]
  }
  return params
}
const fetchList = async () => {
  loading.value = true
  try {
    const params = buildListParams()
    const response = await request({
      url: '/controlTasks',
      method: 'get',
      params
    })
    if (response && response.data) {
      const res = response.data
      tableData.value = Array.isArray(res.list) ? res.list : (res.data || [])
      total.value = typeof res.total === 'number' ? res.total : (res.totalCount || tableData.value.length)
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch (e) {
    tableData.value = []
    total.value = 0
    ElMessage.error('获取列表失败')
  } finally {

    loading.value = false
  }
}
const handleSearch = () => {
  pagination.currentPage = 1
  fetchList()
}
const resetSearch = () => {
  searchQuery.actionType = ''
  searchQuery.status = ''
  searchQuery.deviceId = ''
  searchQuery.zoneName = ''
  searchQuery.dateRange = null
  pagination.currentPage = 1
  fetchList()
}
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.currentPage = 1
  fetchList()
}
const handleCurrentChange = (val) => {
  pagination.currentPage = val
  fetchList()
}
const handleTerminate = (row) => {
  ElMessageBox.confirm(`确定要强行终止设备 ${row.deviceId} 的任务吗？`, '警告', {
    confirmButtonText: '确定终止',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await request({
        url: `/controlTasks/${row.id}/terminate`,
        method: 'post'
      })
      if (response && (response.code === 0 || response.success || response.status === 200 || response.data))
{
        ElMessage.success('已发送终止指令，任务已取消')
        fetchList()
      } else {
        ElMessage.error('终止操作失败')
      }
    } catch (e) {
      ElMessage.error('终止操作失败')
    }
  }).catch(() => {})
}
const handleRetry = async (row) => {
  ElMessage.info('正在下发重试指令...')

  try {
    const response = await request({
      url: `/controlTasks/${row.id}/retry`,
      method: 'post'
    })
    if (response && (response.code === 0 || response.success || response.data)) {
      ElMessage.success('重试指令下发成功，已重新进入排队队列')
      fetchList()
    } else {
      ElMessage.error('重试失败')
    }
  } catch (e) {
    ElMessage.error('重试失败')
  }
}
const openAddDialog = () => {
  dialogType.value = 'add'
  form.id = null
  form.actionType = 'irrigation'
  form.deviceId = ''
  form.zoneName = ''
  form.scheduledAt = ''
  form.status = 'pending'
  dialogVisible.value = true
}
const openEditDialog = (row) => {
  dialogType.value = 'edit'
  Object.assign(form, row)
  dialogVisible.value = true
}
const submitForm = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (err) {
    return
  }
  if (dialogType.value === 'add') {
    try {
      const payload = {
        actionType: form.actionType,
        deviceId: form.deviceId,
        zoneName: form.zoneName,
        scheduledAt: form.scheduledAt
      }
      const response = await request({
        url: '/controlTasks',
        method: 'post',
        data: payload
      })

      if (response && (response.code === 0 || response.success || response.data)) {
        ElMessage.success('控制指令下发成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error('下发失败')
      }
    } catch (e) {
      ElMessage.error('下发失败')
    }
  } else {
    try {
      const payload = {
        actionType: form.actionType,
        deviceId: form.deviceId,
        zoneName: form.zoneName,
        scheduledAt: form.scheduledAt,
        status: form.status
      }
      const response = await request({
        url: `/controlTasks/${form.id}`,
        method: 'put',
        data: payload
      })
      if (response && (response.code === 0 || response.success || response.data)) {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error('修改失败')
      }
    } catch (e) {
      ElMessage.error('修改失败')
    }
  }
}
const handleDelete = async (id) => {
  try {
    const response = await request({
      url: `/controlTasks/${id}`,
      method: 'delete'
    })
    if (response && (response.code === 0 || response.success || response.data)) {
      ElMessage.success('记录已删除')
      if ((pagination.currentPage - 1) * pagination.pageSize >= total.value - 1 && pagination.currentPage > 1) {
        pagination.currentPage = Math.max(1, pagination.currentPage - 1)
      }
      fetchList()
    } else {
      ElMessage.error('删除失败')

    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
onMounted(() => {
  fetchList()
})
</script>
<style scoped>
.irrigation-console {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}
.search-card {
  margin-bottom: 20px;
  border-radius: 8px;
}
.search-form .el-form-item {
  margin-bottom: 0;
  margin-right: 20px;
}
.table-card {
  border-radius: 8px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.right-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.error-text {
  color: var(--el-color-danger);
  font-weight: bold;
}
</style>
