import { Module } from 'vuex'

import { AdminApi } from '@/apis/AdminApi'
import { NewNameRequest } from '@/apis/NewNameRequest'
import { ProjectModel } from '@/apis/ProjectModel'

interface StoreCollections {
  project?: ProjectModel
  collection?: string
}

const collectionsStore: Module<StoreCollections, unknown> = {
  namespaced: true,
  state() {
    return {}
  },
  mutations: {
    setProject(state: StoreCollections, project: ProjectModel) {
      state.project = project
    },
    setCollection(state: StoreCollections, name: string) {
      state.collection = name
    },
    appendCollection(state: StoreCollections, name: string) {
      if (state.project?.collections) {
        state.project.collections = state.project.collections.concat(name)
      }
    },
  },
  actions: {
    async createCollection({state, commit}, request: NewNameRequest) {
      await AdminApi.createCollection({
        project: state.project?.name,
        collection: request.name,
      })

      await commit('appendCollection', request.name)
    },
    async fetchProject(context, name: string) {
      const project = await AdminApi.fetchProject(name)

      context.commit('setProject', project)
      context.commit('setCollection', project.collections?.[0])
    },
  },
  getters: {
    hasCollections(state: StoreCollections): boolean {
      return (state.project?.collections ?? []).length > 0
    },
    getCollections(state: StoreCollections): string[] {
      return state.project?.collections ?? []
    },
  },
}

export default collectionsStore