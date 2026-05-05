<template>
    <div>
        <head-top head-title="订单列表" :go-back='true'></head-top>
        <section class="mt-10">
            <van-pull-refresh v-model="isRefreash" @refresh="onRefresh">
                <!-- <p>刷新次数: {{num}}</p> -->
                <van-list v-model:loading="isLoading" :offset="600" :finished="finished" finished-text="没有更多了"
                    :immediate-check="false" @load="onLoad">
                    <ul>
                        <li v-for="(item, index) in orderList" :key="index" class="bg-white my-2">
                            <section class="flex p-2">
                                <van-image width="2rem" height="2rem" :src="item.shopImage"></van-image>
                                <div class="flex flex-col flex-1 divide-y">
                                    <div class="flex justify-between pl-2 pb-1">
                                        <div class="flex flex-col justify-center text-xxs">
                                            <span class="text-1xs">{{ item.shopName }}</span>
                                            <span class="text-gray-400">{{ item.createTime }}</span>
                                        </div>
                                        <div class="text-xxs">
                                            {{ item.orderStateStr }}
                                        </div>
                                    </div>
                                    <div class="flex justify-between text-xxs pl-2 py-2">
                                        <span class="text-gray-400">{{ item.orderDetailsList[0].goodsName }}{{
                                                item.orderDetailsList.length
                                                    > 1 ? ' 等' + item.orderDetailsList.length + '件商品' : ''
                                        }}</span>
                                        <span class="text-orange-500">¥{{ item.totalPrice.toFixed(2) }}</span>
                                    </div>
                                    <div class="text-right pt-1">
                                        <span @click="gotoComment(item)"
                                            v-if="item.orderState == 304 && item.commentState === false"
                                            class="text-xxs text-orange-500 border border-orange-500 p-1 rounded">{{
                                                    "去评论"
                                            }}</span>
                                        <span @click="lookBackMoney(item)"
                                            v-if="item.orderState == 306"
                                            class="text-xxs text-orange-500 border border-orange-500 p-1 rounded">{{
                                                    "查看退款详情"
                                            }}</span>
                                        <compute-time :order="item" v-if="item.orderState == 102"
                                            :time="item.createTime"></compute-time>
                                        <span @click="gotoBuyAgain(item.orderDetailsList, item.shopId, item.geohash)"
                                            v-if="item.orderState == 304"
                                            class="text-xxs border p-1 rounded text-blue-400 border-blue-400">再来一单</span>
                                            <span
                                            class="text-xxs border p-1 rounded text-blue-400 border-blue-400">订单详情</span>
                                    </div>
                                </div>
                            </section>
                        </li>
                    </ul>
                </van-list>
            </van-pull-refresh>
            <transition name="router-slid" mode="out-in">
                <router-view></router-view>
            </transition>
        </section>
        <van-dialog v-model:show="showAlert">
            <template #default>
                <div class="flex flex-col items-center">
                    <van-icon size="82" class="m-2 text-yellow-400" name="warning-o" />
                    <span class="mb-2">{{alertText}}</span>
                </div>
            </template>
            <template #footer>
                <div class="bg-green-400">
                    <button class="w-full h-6 text-white" @click="showAlert=false">确认</button>
                </div>
            </template>
        </van-dialog>
    </div>
</template>

<script>
import { onMounted, toRefs, reactive, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { imgBaseUrl } from '@/config/env'
import { getOrderList } from '@/api8080/order'
import HeadTop from '@/components/HeadTop'
import ComputeTime from '@/components/computeTime'
import router from '../../router'
export default {
    components: {
        HeadTop,
        ComputeTime
    },
    setup() {
        const router = useRouter()
        const store = useStore()
        const useMutation = store._mutations
        const state = reactive({
            isRefreash: false,  // 下拉刷新
            isLoading: false,   // 上拉加载
            finished: false,    // 数据加载是否完成
            orderList: [],  // 订单列表
            offset: 1,  // 分页数据
            userInfo: null,
            imgBaseUrl,
            geohash: null,
            statement: {},
            alertText:'',
            showAlert:false,
        })
        watch(() => store.state.userInfo, (val) => {
            if (val && val.user_id) {
                initData()
            }
        })
        onMounted(() => {
            if (store.state.userInfo == null) {
                router.push({ path: '/login', replace: true })
            }
            onRefresh()
        })
        const initData = async () => {
            state.geohash = store.state.geohash
            state.userInfo = store.state.userInfo
            if (store.state.userInfo && store.state.userInfo.id) {
                let res = await getOrderList(state.offset, 20, state.userInfo.id)
                console.log("订单表数据:")
                console.log(res.data.data.rows);
                if (state.orderList.length > 0) {
                    state.orderList = [...state.orderList, ...res.data.data.rows]
                } else {
                    state.orderList = res.data.data.rows;
                }
                if (res.data.data.rows.length < 20) {
                    state.finished = true
                }
            }
            state.isLoading = false
        }
        const onRefresh = () => {
            state.orderList = []
            state.offset = 1
            state.isRefreash = false
            state.finished = false
            initData()
        }
        const onLoad = () => {
            state.offset += 1
            initData()
        }
        const gotoBuyAgain = (item, shopId) => {
            console.log("再来一单", item, shopId)
            //将信息保存至vuex
            useMutation.BUY_AGAIN[0]({ orderDetailsList: item, shopId: shopId })
            router.push({ replace: true, path: '/shop', query: { geohash: store.state.geohash, id: shopId } })
        }
        const gotoComment = (item) => {
            router.push({ replace: true, path: '/order/addcomment', query: { orderId: item.id } })
        }
        const lookBackMoney = ()=>{
            state.showAlert = true
            state.alertText = "退款将在三个工作日内返还给您"
        }
        return {
            ...toRefs(state),
            initData,
            onRefresh,
            onLoad,
            gotoBuyAgain,
            gotoComment,
            lookBackMoney
        }
    }
}
</script>

<style scoped>

</style>