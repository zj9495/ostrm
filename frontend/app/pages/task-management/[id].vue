<!--
  Ostrm - Stream Management System
  Copyright (C) 2024 Ostrm Project

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->

<template>
  <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center py-20">
      <div class="text-center">
        <div class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent"></div>
        <p class="mt-4 text-white/50 text-lg">加载中...</p>
      </div>
    </div>

    <template v-else>
      <!-- 配置信息卡片 -->
      <div class="card mb-6 animate-fade-in">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-lg leading-6 font-medium text-white">配置信息</h3>
            <p class="mt-1 max-w-2xl text-sm text-white/40">
              {{ isLocalSource ? '本地文件数据源配置详情' : '当前 OpenList 配置详情' }}
            </p>
          </div>
          <div class="flex items-center gap-2">
            <span :class="isLocalSource ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-blue-500/20 text-blue-400 border border-blue-500/30'" class="px-2 py-0.5 rounded-full text-xs font-medium">
              {{ isLocalSource ? '本地文件' : 'OpenList' }}
            </span>
            <span :class="configInfo?.isActive ? 'badge-success' : 'badge-neutral'">
              {{ configInfo?.isActive ? '启用' : '禁用' }}
            </span>
          </div>
        </div>

        <div class="mt-5 border-t border-white/6 pt-5" v-if="configInfo">
          <template v-if="isLocalSource">
            <dl class="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
              <div>
                <dt class="text-sm text-white/40">数据源类型</dt>
                <dd class="mt-1 text-sm text-white">本地文件系统</dd>
              </div>
              <div>
                <dt class="text-sm text-white/40">创建时间</dt>
                <dd class="mt-1 text-sm text-white">{{ formatDate(configInfo.createdAt) }}</dd>
              </div>
            </dl>
          </template>
          <template v-else>
            <dl class="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
              <div>
                <dt class="text-sm text-white/40">配置名称</dt>
                <dd class="mt-1 text-sm text-white">{{ configInfo.username }}</dd>
              </div>
              <div>
                <dt class="text-sm text-white/40">Base URL</dt>
                <dd class="mt-1 text-sm text-white break-all font-mono">{{ configInfo.baseUrl }}</dd>
              </div>
              <div>
                <dt class="text-sm text-white/40">Base Path</dt>
                <dd class="mt-1 text-sm text-white">{{ configInfo.basePath || '/' }}</dd>
              </div>
              <div>
                <dt class="text-sm text-white/40">创建时间</dt>
                <dd class="mt-1 text-sm text-white">{{ formatDate(configInfo.createdAt) }}</dd>
              </div>
            </dl>
          </template>
        </div>
      </div>

      <!-- 任务管理区域 -->
      <div class="card animate-fade-in" style="animation-delay: 0.1s">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-lg leading-6 font-medium text-white">任务管理</h3>
            <p class="mt-1 text-sm text-white/40">管理您的 STRM 生成任务</p>
          </div>
          <button type="button" class="btn-primary" @click="showCreateTaskModal = true">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
            </svg>
            创建任务
          </button>
        </div>

        <!-- 任务列表 -->
        <div class="space-y-4" v-if="tasks.length > 0">
          <div class="card" v-for="task in tasks" :key="task.id">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between mb-4 gap-3">
              <h4 class="text-lg font-medium text-white">{{ task.taskName }}</h4>
              <div class="flex items-center space-x-2 flex-wrap gap-y-2">
                <span :class="task.isActive ? 'badge-success' : 'badge-neutral'" class="text-xs">
                  {{ task.isActive ? '启用' : '禁用' }}
                </span>
                <button class="btn-icon" @click="editTask(task)" title="编辑">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                  </svg>
                </button>
                <button class="btn-icon text-emerald-400 hover:text-emerald-300" @click="showExecuteModal(task.id)" :disabled="generatingStrm[task.id]" title="立即执行">
                  <svg v-if="generatingStrm[task.id]" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </button>
                <button class="btn-icon text-sky-400 hover:text-sky-300" @click="openRunHistory(task)" title="运行记录">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </button>
                <button class="btn-icon text-red-400 hover:text-red-300" @click="deleteTask(task.id)" title="删除">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                  </svg>
                </button>
              </div>
            </div>

            <div class="grid grid-cols-1 gap-x-4 gap-y-3 sm:grid-cols-2">
              <div>
                <dt class="text-sm text-white/40">路径</dt>
                <dd class="mt-1 text-sm text-white/80 break-all font-mono">{{ task.path }}</dd>
              </div>
              <div>
                <dt class="text-sm text-white/40">STRM路径</dt>
                <dd class="mt-1 text-sm text-white/80 break-all font-mono">{{ task.strmPath }}</dd>
              </div>
              <div>
                <dt class="text-sm text-white/40">定时任务</dt>
                <dd class="mt-1 text-sm text-white/80">{{ task.cron || '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-sm text-white/40">上次执行</dt>
                <dd class="mt-1 text-sm text-white/80">{{ formatDate(task.lastExecTime) }}</dd>
              </div>
            </div>

            <div class="mt-3 flex items-center space-x-4 flex-wrap gap-y-2">
              <label class="flex items-center text-sm text-white/60">
                <input type="checkbox" :checked="task.needScrap" disabled class="mr-2 h-4 w-4 rounded border-white/20 bg-white/5 text-blue-500">
                需要刮削
              </label>
              <label class="flex items-center text-sm text-white/60">
                <input type="checkbox" :checked="task.isIncrement" disabled class="mr-2 h-4 w-4 rounded border-white/20 bg-white/5 text-blue-500">
                增量更新
              </label>
              <label class="flex items-center text-sm text-white/60">
                <input type="checkbox" :checked="task.generateSign !== false" disabled class="mr-2 h-4 w-4 rounded border-white/20 bg-white/5 text-blue-500">
                生成 sign 参数
              </label>
            </div>

            <div class="mt-3" v-if="task.renameRegex">
              <dt class="text-sm text-white/40">重命名正则表达式</dt>
              <dd class="mt-1 text-sm text-white/80 font-mono bg-white/5 px-3 py-2 rounded break-all">{{ task.renameRegex }}</dd>
            </div>

            <div class="mt-3" v-if="hasTaskFilters(task)">
              <dt class="text-sm text-white/40">过滤规则</dt>
              <div class="mt-1 space-y-2">
                <dd v-if="task.minFileSizeBytes !== null && task.minFileSizeBytes !== undefined" class="text-sm text-white/80 font-mono bg-white/5 px-3 py-2 rounded break-all">
                  最小文件大小: {{ formatFileSize(task.minFileSizeBytes) }}
                </dd>
                <dd v-if="task.fileNameExcludeRegex" class="text-sm text-white/80 font-mono bg-white/5 px-3 py-2 rounded break-all">
                  文件名排除: {{ task.fileNameExcludeRegex }}
                </dd>
                <dd v-if="task.directoryNameExcludeRegex" class="text-sm text-white/80 font-mono bg-white/5 px-3 py-2 rounded break-all">
                  目录名排除: {{ task.directoryNameExcludeRegex }}
                </dd>
              </div>
            </div>

            <div class="mt-3" v-if="task.strmUrlReplaceFrom">
              <dt class="text-sm text-white/40">STRM 内容地址替换</dt>
              <dd class="mt-1 text-sm text-white/80 font-mono bg-white/5 px-3 py-2 rounded break-all">
                {{ task.strmUrlReplaceFrom }} → {{ task.strmUrlReplaceTo }}
              </dd>
            </div>

            <div class="mt-3 text-xs text-white/30">
              创建时间: {{ formatDate(task.createdAt) }}
              <span v-if="latestRunIds[task.id]" class="ml-3">最近提交运行ID: {{ latestRunIds[task.id] }}</span>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div class="text-center py-12" v-else>
          <div class="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path>
            </svg>
          </div>
          <h3 class="text-lg font-medium text-white mb-2">暂无任务配置</h3>
          <p class="text-white/40 mb-6">创建您的第一个任务配置</p>
          <button type="button" class="btn-primary" @click="showCreateTaskModal = true">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
            </svg>
            创建第一个任务
          </button>
        </div>
      </div>
    </template>

    <!-- 创建/编辑任务模态框 -->
    <Teleport to="body">
      <div v-if="showCreateTaskModal || showEditTaskModal" class="modal-overlay animate-fade-in">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="modal-content animate-scale-in flex max-h-[85vh] w-full max-w-5xl flex-col overflow-hidden" @click.stop>
            <div class="flex shrink-0 items-center justify-between mb-6">
              <h3 class="text-xl font-semibold text-white">
                {{ showCreateTaskModal ? '创建任务' : '编辑任务' }}
              </h3>
              <button @click="closeModal" class="btn-icon">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            <form @submit.prevent="submitTask" class="min-h-0 flex flex-1 flex-col">
              <div class="min-h-0 flex-1 overflow-y-auto pr-2">
                <div class="grid grid-cols-1 gap-5 lg:grid-cols-2">
                  <div>
                    <label class="block text-sm text-white/70 mb-2">任务名称 *</label>
                    <input v-model="taskForm.taskName" type="text" required class="input-field" placeholder="请输入任务名称">
                  </div>

                  <div>
                    <label class="block text-sm text-white/70 mb-2">任务路径 *</label>
                    <template v-if="isLocalSource">
                      <div class="flex gap-2">
                        <input v-model="taskForm.path" type="text" required class="input-field flex-1" placeholder="点击右侧按钮选择本地目录" readonly>
                        <button type="button" @click="openLocalTreePicker" class="btn-secondary whitespace-nowrap">
                          选择目录
                        </button>
                      </div>
                    </template>
                    <template v-else>
                      <input v-model="taskForm.path" type="text" required class="input-field" placeholder="请输入OpenList中的媒体路径">
                    </template>
                  </div>

                  <div class="lg:col-span-2">
                    <label class="block text-sm text-white/70 mb-2">STRM路径</label>
                    <div class="flex">
                      <span class="inline-flex items-center px-3 rounded-l-xl border border-r-0 border-white/10 bg-white/5 text-white/50 text-sm">
                        /app/backend/strm/
                      </span>
                      <input v-model="strmSubPath" type="text" placeholder="子路径（可选）" class="input-field rounded-l-none">
                    </div>
                    <p class="mt-1 text-xs text-white/30">前缀 /app/backend/strm/ 固定不可修改</p>
                  </div>

                  <div>
                    <label class="block text-sm text-white/70 mb-2">定时任务表达式</label>
                    <input v-model="taskForm.cron" type="text" placeholder="例如: 0 15 10 ? * *" class="input-field">
                    <p class="mt-1 text-xs text-white/30">Cron表达式格式，留空表示不启用定时任务</p>
                  </div>

                  <div>
                    <div class="flex items-center justify-between mb-2">
                      <label class="block text-sm text-white/70">重命名正则表达式</label>
                      <button type="button" @click="showRenameRegexHelp = !showRenameRegexHelp" class="text-white/40 hover:text-blue-400 transition-colors">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                      </button>
                    </div>
                    <input v-model="taskForm.renameRegex" type="text" placeholder="留空表示不需要重命名" class="input-field">
                    <p class="mt-1 text-xs text-white/30">用于文件重命名的正则表达式</p>

                    <div v-if="showRenameRegexHelp" class="mt-3 p-4 bg-blue-500/10 border border-blue-500/20 rounded-xl">
                      <h4 class="text-sm font-medium text-blue-400 mb-2">使用说明</h4>
                      <div class="text-xs text-white/70 space-y-2">
                        <p><strong>格式：</strong>原始模式|替换内容</p>
                        <p><strong>示例：</strong></p>
                        <ul class="list-disc list-inside ml-2 space-y-1">
                          <li>移除方括号：<code class="bg-white/10 px-1 rounded">[\[\]()]|</code></li>
                          <li>空格转下划线：<code class="bg-white/10 px-1 rounded">\s+|_</code></li>
                          <li>添加前缀：<code class="bg-white/10 px-1 rounded">^|Movie_</code></li>
                        </ul>
                      </div>
                    </div>
                  </div>

                  <div class="space-y-4 rounded-xl border border-white/10 bg-white/5 p-4">
                    <h4 class="text-sm font-medium text-white/80">过滤规则</h4>
                    <div>
                      <label class="block text-sm text-white/70 mb-2">最小文件大小 (MB)</label>
                      <input v-model="minFileSizeMb" type="number" min="0" step="1" placeholder="留空表示不过滤文件大小" class="input-field">
                    </div>
                    <div>
                      <label class="block text-sm text-white/70 mb-2">文件名排除正则</label>
                      <input v-model="taskForm.fileNameExcludeRegex" type="text" placeholder="匹配该正则的文件会被跳过" class="input-field">
                    </div>
                    <div>
                      <label class="block text-sm text-white/70 mb-2">目录名称排除正则</label>
                      <input v-model="taskForm.directoryNameExcludeRegex" type="text" placeholder="匹配该正则的目录会被跳过" class="input-field">
                    </div>
                  </div>

                  <div class="space-y-4 rounded-xl border border-white/10 bg-white/5 p-4">
                    <h4 class="text-sm font-medium text-white/80">STRM URL 选项</h4>
                    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                      <div>
                        <label class="block text-sm text-white/70 mb-2">内容地址替换来源</label>
                        <input v-model="taskForm.strmUrlReplaceFrom" type="text" placeholder="例如: http://host:port/d" class="input-field">
                      </div>
                      <div>
                        <label class="block text-sm text-white/70 mb-2">内容地址替换目标</label>
                        <input v-model="taskForm.strmUrlReplaceTo" type="text" placeholder="例如: /mnt/...url=" class="input-field">
                      </div>
                    </div>
                    <p class="text-xs text-white/30">在 Base URL 替换后的 STRM 内容地址中精确替换指定字符串，留空表示不替换</p>

                    <label class="flex items-start cursor-pointer">
                      <input v-model="taskForm.generateSign" type="checkbox" class="mt-1 h-4 w-4 rounded border-white/20 bg-white/5 text-blue-500">
                      <span class="ml-2 text-sm text-white/70">
                        生成 sign 参数
                        <span class="block text-xs text-white/40 mt-0.5">在 STRM URL 中追加 sign 查询参数</span>
                      </span>
                    </label>
                  </div>

                  <div class="space-y-3 rounded-xl border border-white/10 bg-white/5 p-4 lg:col-span-2">
                    <label class="flex items-start cursor-pointer">
                      <input v-model="taskForm.needScrap" type="checkbox" class="mt-1 h-4 w-4 rounded border-white/20 bg-white/5 text-blue-500">
                      <span class="ml-2 text-sm text-white/70">
                        需要刮削
                        <span class="block text-xs text-white/40 mt-0.5">启用TMDB刮削功能，生成NFO和封面</span>
                      </span>
                    </label>

                    <label class="flex items-center cursor-pointer">
                      <input v-model="taskForm.isIncrement" type="checkbox" class="h-4 w-4 rounded border-white/20 bg-white/5 text-blue-500">
                      <span class="ml-2 text-sm text-white/70">增量更新</span>
                    </label>

                    <label class="flex items-center cursor-pointer">
                      <input v-model="taskForm.isActive" type="checkbox" class="h-4 w-4 rounded border-white/20 bg-white/5 text-blue-500">
                      <span class="ml-2 text-sm text-white/70">启用任务</span>
                    </label>
                  </div>
                </div>
              </div>

              <div class="flex shrink-0 justify-end gap-3 border-t border-white/10 pt-4 mt-4">
                <button type="button" @click="closeModal" class="btn-secondary">取消</button>
                <button type="submit" :disabled="submitting" class="btn-primary">
                  <svg v-if="submitting" class="loading-spinner -ml-1 mr-2 w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ submitting ? '保存中...' : (showCreateTaskModal ? '创建' : '保存') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 执行模式选择模态框 -->
    <Teleport to="body">
      <div v-if="showExecuteTaskModal" class="modal-overlay animate-fade-in">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="modal-content animate-scale-in w-full max-w-md" @click.stop>
            <div class="flex items-center justify-between mb-6">
              <h3 class="text-xl font-semibold text-white">选择执行模式</h3>
              <button @click="closeExecuteModal" class="btn-icon">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            <div class="space-y-4">
              <p class="text-sm text-white/50">请选择任务执行模式：</p>

              <button @click="executeTask(currentTaskId, false)" class="w-full flex items-center justify-between p-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all cursor-pointer">
                <div class="text-left">
                  <div class="font-medium text-white">全量执行</div>
                  <div class="text-sm text-white/40">清空STRM目录，重新生成所有文件</div>
                </div>
                <svg class="w-5 h-5 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                </svg>
              </button>

              <button @click="executeTask(currentTaskId, true)" class="w-full flex items-center justify-between p-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all cursor-pointer">
                <div class="text-left">
                  <div class="font-medium text-white">增量执行</div>
                  <div class="text-sm text-white/40">只处理变化的文件，清理孤立文件</div>
                </div>
                <svg class="w-5 h-5 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 运行记录模态框 -->
    <Teleport to="body">
      <div v-if="showRunHistoryModal" class="modal-overlay animate-fade-in">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="modal-content animate-scale-in w-full max-w-4xl max-h-[85vh] overflow-hidden flex flex-col" @click.stop>
            <div class="flex items-center justify-between mb-6">
              <div>
                <h3 class="text-xl font-semibold text-white">运行记录</h3>
                <p class="mt-1 text-sm text-white/40">{{ selectedTaskForRuns?.taskName }}</p>
              </div>
              <button @click="closeRunHistory" class="btn-icon">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            <div class="overflow-y-auto pr-1">
              <div v-if="loadingTaskRuns" class="flex justify-center items-center py-12">
                <div class="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-500 border-t-transparent"></div>
              </div>

              <div v-else-if="taskRuns.length === 0" class="text-center py-12 text-white/40">
                暂无运行记录
              </div>

              <div v-else class="space-y-3">
                <div v-for="run in taskRuns" :key="run.id" class="rounded-lg border border-white/10 bg-white/5 p-4">
                  <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <div class="min-w-0">
                      <div class="flex items-center gap-2 flex-wrap">
                        <span :class="getRunStatusClass(run.status)" class="text-xs">
                          {{ formatRunStatus(run.status) }}
                        </span>
                        <span class="text-xs text-white/50">#{{ run.id }}</span>
                        <span class="text-xs text-white/50">{{ run.isIncrement ? '增量' : '全量' }}</span>
                      </div>
                      <dl class="mt-3 grid grid-cols-1 gap-x-4 gap-y-2 sm:grid-cols-2 lg:grid-cols-4">
                        <div>
                          <dt class="text-xs text-white/40">提交时间</dt>
                          <dd class="mt-1 text-sm text-white/80">{{ formatOptionalDate(run.submittedAt, '未提交') }}</dd>
                        </div>
                        <div>
                          <dt class="text-xs text-white/40">开始时间</dt>
                          <dd class="mt-1 text-sm text-white/80">{{ formatOptionalDate(run.startedAt, '未开始') }}</dd>
                        </div>
                        <div>
                          <dt class="text-xs text-white/40">结束时间</dt>
                          <dd class="mt-1 text-sm text-white/80">{{ formatOptionalDate(run.finishedAt, '未结束') }}</dd>
                        </div>
                        <div>
                          <dt class="text-xs text-white/40">耗时</dt>
                          <dd class="mt-1 text-sm text-white/80">{{ formatDuration(run.durationMs) }}</dd>
                        </div>
                      </dl>
                      <div v-if="run.errorMessage" class="mt-3 rounded bg-red-500/10 px-3 py-2 text-sm text-red-200 break-all">
                        {{ run.errorMessage }}
                      </div>
                    </div>
                    <button class="btn-secondary shrink-0" @click="openRunLog(run)">查看日志</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 单次运行日志模态框 -->
    <Teleport to="body">
      <div v-if="showRunLogModal" class="modal-overlay animate-fade-in">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="modal-content animate-scale-in w-full max-w-5xl max-h-[85vh] overflow-hidden flex flex-col" @click.stop>
            <div class="flex items-center justify-between mb-6">
              <div>
                <h3 class="text-xl font-semibold text-white">运行日志</h3>
                <p class="mt-1 text-sm text-white/40">运行记录 #{{ selectedRun?.id }}</p>
              </div>
              <button @click="closeRunLog" class="btn-icon">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            <div class="overflow-y-auto rounded-lg border border-white/10 bg-black/20 p-4 font-mono text-xs">
              <div v-if="loadingRunLogs" class="flex justify-center items-center py-12">
                <div class="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-500 border-t-transparent"></div>
              </div>

              <div v-else-if="taskRunLogs.length === 0" class="text-center py-12 text-white/40">
                暂无运行日志
              </div>

              <div v-else class="space-y-2">
                <div v-for="logLine in taskRunLogs" :key="logLine.id" class="grid grid-cols-1 gap-1 text-white/70 md:grid-cols-[180px_70px_minmax(0,1fr)]">
                  <span class="text-white/40">{{ formatOptionalDate(logLine.loggedAt, '未记录') }}</span>
                  <span :class="getLogLevelClass(logLine.level)" class="font-semibold">{{ logLine.level }}</span>
                  <span class="min-w-0 whitespace-pre-wrap break-words leading-relaxed">{{ logLine.message }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 本地目录树选择器模态框 -->
    <Teleport to="body">
      <div v-if="showLocalTreePicker" class="modal-overlay animate-fade-in">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="modal-content animate-scale-in w-full max-w-lg max-h-[70vh] overflow-hidden flex flex-col" @click.stop>
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-xl font-semibold text-white">选择本地目录</h3>
              <button @click="showLocalTreePicker = false" class="btn-icon">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            <!-- 面包屑导航 -->
            <div class="flex items-center gap-1 text-sm text-white/60 mb-3 flex-wrap">
              <button @click="navigateToLocalBreadcrumb(-1)" class="hover:text-blue-400 transition-colors">根目录</button>
              <span v-for="(crumb, index) in localTreePath" :key="index" class="flex items-center gap-1">
                <span class="text-white/30">/</span>
                <button @click="navigateToLocalBreadcrumb(index)" class="hover:text-blue-400 transition-colors">{{ crumb.name }}</button>
              </span>
            </div>

            <div class="flex-1 overflow-y-auto rounded-lg border border-white/10 bg-white/5">
              <div v-if="localTreeLoading" class="flex justify-center items-center py-12">
                <div class="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-500 border-t-transparent"></div>
              </div>

              <div v-else-if="localTreeNodes.length === 0" class="text-center py-12 text-white/40">
                此目录下没有子目录
              </div>

              <div v-else class="divide-y divide-white/5">
                <div v-for="node in localTreeNodes" :key="node.path"
                     class="flex items-center justify-between px-4 py-3 hover:bg-white/5 transition-colors">
                  <div class="flex items-center gap-3 min-w-0">
                    <svg class="w-5 h-5 text-amber-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                    </svg>
                    <span class="text-sm text-white/80 truncate">{{ node.name }}</span>
                  </div>
                  <div class="flex items-center gap-2 shrink-0">
                    <button v-if="node.hasChildren" @click="navigateIntoLocalDir(node)"
                            class="text-xs text-blue-400 hover:text-blue-300 px-2 py-1 rounded bg-blue-500/10">
                      展开
                    </button>
                    <button @click="selectLocalPath(node)"
                            class="text-xs text-emerald-400 hover:text-emerald-300 px-2 py-1 rounded bg-emerald-500/10">
                      选择
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="taskForm.path" class="mt-3 p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
              <p class="text-sm text-emerald-300">已选择: <span class="font-mono">{{ taskForm.path }}</span></p>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import logger from '~/core/utils/logger'
import { useRoute, useRouter } from 'vue-router'
import { apiCall, authenticatedApiCall } from '~/core/api/client'

const route = useRoute()
const router = useRouter()
const configId = route.params.id

const configInfo = ref(null)
const tasks = ref([])
const loading = ref(true)
const showCreateTaskModal = ref(false)
const showEditTaskModal = ref(false)
const showExecuteTaskModal = ref(false)
const showRunHistoryModal = ref(false)
const showRunLogModal = ref(false)
const submitting = ref(false)
const editingTaskId = ref(null)
const currentTaskId = ref(null)
const generatingStrm = ref({})
const latestRunIds = ref({})
const selectedTaskForRuns = ref(null)
const selectedRun = ref(null)
const taskRuns = ref([])
const taskRunLogs = ref([])
const loadingTaskRuns = ref(false)
const loadingRunLogs = ref(false)
const BYTES_PER_MB = 1024 * 1024
const taskForm = ref({
  taskName: '',
  path: '',
  strmPath: '/app/backend/strm',
  cron: '',
  needScrap: false,
  renameRegex: '',
  isIncrement: true,
  isActive: true,
  minFileSizeBytes: null,
  fileNameExcludeRegex: '',
  directoryNameExcludeRegex: '',
  strmUrlReplaceFrom: '',
  strmUrlReplaceTo: '',
  generateSign: true
})
const strmSubPath = ref('')
const minFileSizeMb = ref('')
const showRenameRegexHelp = ref(false)
// Local directory tree state
const localTreeNodes = ref([])
const localTreePath = ref([])
const localTreeLoading = ref(false)
const showLocalTreePicker = ref(false)

const isLocalSource = computed(() => configInfo.value?.sourceType === 'LOCAL')

const getConfigInfo = async () => {
  try {
    loading.value = true
    const response = await authenticatedApiCall(`/openlist-config/${configId}`, { method: 'GET' })
    if (response.code === 200) {
      configInfo.value = response.data
    } else {
      logger.error('获取配置信息失败:', response.message)
      await navigateTo('/')
    }
  } catch (error) {
    logger.error('获取配置信息时发生错误:', error)
    await navigateTo('/')
  } finally {
    loading.value = false
  }
}

const fetchTasks = async () => {
  try {
    const response = await authenticatedApiCall('/task-config', { method: 'GET' })
    if (response.code === 200) {
      tasks.value = response.data.filter(task => task.openlistConfigId == configId)
    }
  } catch (error) {
    logger.error('获取任务列表失败:', error)
  }
}

const resetTaskForm = () => {
  taskForm.value = {
    taskName: '', path: '', strmPath: '/app/backend/strm', cron: '',
    needScrap: false, renameRegex: '', isIncrement: true, isActive: true,
    minFileSizeBytes: null, fileNameExcludeRegex: '', directoryNameExcludeRegex: '',
    strmUrlReplaceFrom: '', strmUrlReplaceTo: '', generateSign: true
  }
  strmSubPath.value = ''
  minFileSizeMb.value = ''
  showRenameRegexHelp.value = false
}

const editTask = (task) => {
  editingTaskId.value = task.id
  taskForm.value = {
    taskName: task.taskName, path: task.path, strmPath: task.strmPath,
    cron: toTextValue(task.cron), needScrap: task.needScrap === true,
    renameRegex: toTextValue(task.renameRegex), isIncrement: task.isIncrement, isActive: task.isActive,
    minFileSizeBytes: task.minFileSizeBytes,
    fileNameExcludeRegex: toTextValue(task.fileNameExcludeRegex),
    directoryNameExcludeRegex: toTextValue(task.directoryNameExcludeRegex),
    strmUrlReplaceFrom: toTextValue(task.strmUrlReplaceFrom),
    strmUrlReplaceTo: toTextValue(task.strmUrlReplaceTo),
    generateSign: task.generateSign !== false
  }
  const prefix = '/app/backend/strm/'
  strmSubPath.value = task.strmPath?.startsWith(prefix) ? task.strmPath.substring(prefix.length) : ''
  minFileSizeMb.value = task.minFileSizeBytes === null || task.minFileSizeBytes === undefined ? '' : task.minFileSizeBytes / BYTES_PER_MB
  showEditTaskModal.value = true
}

const validateTaskPath = async (taskPath) => {
  try {
    if (!configInfo.value) throw new Error('配置信息未加载')
    const body = isLocalSource.value
      ? { sourceType: 'LOCAL', taskPath }
      : { baseUrl: configInfo.value.baseUrl, token: configInfo.value.token, basePath: configInfo.value.basePath, taskPath }
    const response = await authenticatedApiCall('/openlist-config/validate-path', {
      method: 'POST',
      body
    })
    if (response.code !== 200) throw new Error(response.message || '路径验证失败')
  } catch (error) {
    throw new Error(error.message || '路径验证失败，请检查路径是否正确')
  }
}

const submitTask = async () => {
  try {
    submitting.value = true
    if (taskForm.value.path) await validateTaskPath(taskForm.value.path)
    const strmSubPathValue = strmSubPath.value === '' ? '' : strmSubPath.value
    const fullStrmPath = '/app/backend/strm/' + strmSubPathValue
    const minFileSizeBytes = parseMinFileSizeBytes()
    const taskData = {
      ...taskForm.value,
      strmPath: fullStrmPath,
      minFileSizeBytes,
      openlistConfigId: parseInt(configId)
    }
    let response
    if (showCreateTaskModal.value) {
      response = await authenticatedApiCall('/task-config', { method: 'POST', body: taskData })
    } else {
      response = await authenticatedApiCall(`/task-config/${editingTaskId.value}`, { method: 'PUT', body: taskData })
    }
    if (response.code === 200) {
      await fetchTasks()
      closeModal()
    } else {
      throw new Error(response.message || '操作失败')
    }
  } catch (error) {
    logger.error('提交任务失败:', error)
    alert(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const deleteTask = async (taskId) => {
  if (!confirm('确定要删除这个任务吗？')) return
  try {
    const response = await authenticatedApiCall(`/task-config/${taskId}`, { method: 'DELETE' })
    if (response.code === 200) await fetchTasks()
    else throw new Error(response.message || '删除失败')
  } catch (error) {
    logger.error('删除任务失败:', error)
  }
}

const closeModal = () => {
  showCreateTaskModal.value = false
  showEditTaskModal.value = false
  editingTaskId.value = null
  resetTaskForm()
}

const formatDate = (timestamp) => !timestamp || timestamp === 0 ? '未执行' : new Date(timestamp).toLocaleString('zh-CN')
const formatFileSize = (bytes) => `${bytes} bytes (${bytes / BYTES_PER_MB} MB)`
const hasTaskFilters = (task) => {
  if (task.minFileSizeBytes !== null && task.minFileSizeBytes !== undefined) {
    return true
  }
  if (Boolean(task.fileNameExcludeRegex)) {
    return true
  }
  if (Boolean(task.directoryNameExcludeRegex)) {
    return true
  }
  return false
}
const toTextValue = (value) => value === null || value === undefined ? '' : value
const parseMinFileSizeBytes = () => {
  if (minFileSizeMb.value === '') {
    return null
  }
  const minFileSizeMbNumber = Number(minFileSizeMb.value)
  if (!Number.isInteger(minFileSizeMbNumber)) {
    throw new Error('最小文件大小请输入整数 MB')
  }
  if (minFileSizeMbNumber < 0) {
    throw new Error('最小文件大小不能小于 0 MB')
  }
  return minFileSizeMbNumber * BYTES_PER_MB
}
const formatOptionalDate = (value, emptyText) => {
  if (value == null) return emptyText
  if (value === 0) return emptyText
  return new Date(value).toLocaleString('zh-CN')
}

const formatDuration = (durationMs) => {
  if (durationMs == null) return '未完成'
  if (durationMs < 1000) return `${durationMs} ms`
  const seconds = Math.floor(durationMs / 1000)
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  if (minutes === 0) return `${seconds} 秒`
  return `${minutes} 分 ${remainingSeconds} 秒`
}

const formatRunStatus = (status) => {
  const labels = {
    SUBMITTED: '已提交',
    RUNNING: '运行中',
    SUCCESS: '成功',
    FAILED: '失败'
  }
  return labels[status]
}

const getRunStatusClass = (status) => {
  const classes = {
    SUBMITTED: 'badge-info',
    RUNNING: 'badge-warning',
    SUCCESS: 'badge-success',
    FAILED: 'badge-error'
  }
  return classes[status]
}

const getLogLevelClass = (level) => {
  const classes = {
    INFO: 'text-blue-300',
    WARN: 'text-amber-300',
    ERROR: 'text-red-300',
    DEBUG: 'text-white/40'
  }
  return classes[level]
}

const showExecuteModal = (taskId) => {
  currentTaskId.value = taskId
  showExecuteTaskModal.value = true
}

const closeExecuteModal = () => {
  showExecuteTaskModal.value = false
  currentTaskId.value = null
}

const executeTask = async (taskId, isIncremental) => {
  try {
    generatingStrm.value[taskId] = true
    closeExecuteModal()
    const response = await authenticatedApiCall(`/task-config/${taskId}/submit`, {
      method: 'POST',
      body: { isIncremental }
    })
    if (response.code === 200) {
      latestRunIds.value[taskId] = response.data.runId
      const modeText = isIncremental ? '增量' : '全量'
      alert(`任务已提交，正在后台执行${modeText}生成STRM文件，运行记录ID: ${response.data.runId}`)
      if (selectedTaskForRuns.value?.id === taskId) {
        await fetchTaskRuns(taskId)
      }
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    logger.error('提交任务失败:', error)
    alert(error.message)
  } finally {
    generatingStrm.value[taskId] = false
  }
}

const openRunHistory = async (task) => {
  selectedTaskForRuns.value = task
  showRunHistoryModal.value = true
  await fetchTaskRuns(task.id)
}

const closeRunHistory = () => {
  showRunHistoryModal.value = false
  selectedTaskForRuns.value = null
  taskRuns.value = []
}

const fetchTaskRuns = async (taskId) => {
  try {
    loadingTaskRuns.value = true
    const response = await authenticatedApiCall(`/task-config/${taskId}/runs`, { method: 'GET' })
    if (response.code === 200) {
      taskRuns.value = response.data
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    logger.error('获取运行记录失败:', error)
    alert(error.message)
  } finally {
    loadingTaskRuns.value = false
  }
}

const openRunLog = async (run) => {
  selectedRun.value = run
  showRunLogModal.value = true
  await fetchRunLogs(run.id)
}

const closeRunLog = () => {
  showRunLogModal.value = false
  selectedRun.value = null
  taskRunLogs.value = []
}

const fetchRunLogs = async (runId) => {
  try {
    loadingRunLogs.value = true
    const response = await authenticatedApiCall(`/task-runs/${runId}/logs`, { method: 'GET' })
    if (response.code === 200) {
      taskRunLogs.value = response.data
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    logger.error('获取运行日志失败:', error)
    alert(error.message)
  } finally {
    loadingRunLogs.value = false
  }
}

// Local directory tree functions
const openLocalTreePicker = async () => {
  showLocalTreePicker.value = true
  localTreePath.value = []
  await loadLocalTreeNodes(null)
}

const loadLocalTreeNodes = async (parentPath) => {
  try {
    localTreeLoading.value = true
    const params = parentPath ? `?parentPath=${encodeURIComponent(parentPath)}` : ''
    const response = await authenticatedApiCall(`/openlist-config/${configId}/local-directory-tree${params}`, { method: 'GET' })
    if (response.code === 200) {
      localTreeNodes.value = response.data || []
    } else {
      localTreeNodes.value = []
    }
  } catch (error) {
    logger.error('加载目录树失败:', error)
    localTreeNodes.value = []
  } finally {
    localTreeLoading.value = false
  }
}

const navigateIntoLocalDir = async (node) => {
  localTreePath.value.push({ name: node.name, path: node.path })
  await loadLocalTreeNodes(node.path)
}

const navigateToLocalBreadcrumb = async (index) => {
  if (index < 0) {
    localTreePath.value = []
    await loadLocalTreeNodes(null)
  } else {
    localTreePath.value = localTreePath.value.slice(0, index + 1)
    await loadLocalTreeNodes(localTreePath.value[index].path)
  }
}

const selectLocalPath = (node) => {
  taskForm.value.path = node.path
  showLocalTreePicker.value = false
}

onMounted(() => {
  getConfigInfo()
  fetchTasks()
})
</script>
