import { createRouter, createWebHashHistory } from 'vue-router'
import App from '../App.vue'
// createWebHashHistory Hash路由 #
// createWebHistory History路由 
// createMemoryHistory  带缓存的History路由
const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/',
            name: 'index',
            // 路由懒加载
            component: App, // 顶层路由
            children: [ // 二级路由
                // 地址为空时重定位到首页
                {
                    path: '',
                    redirect: '/home'
                },
                {
                    path: 'shopChat', // 联系商家
                    component: () => import('../pages/shop/shopChat/ShopChat'),
                    meta: { keepAlive: false },
                },
                {
                    path: '/home',  // 首页（城市列表）
                    component: () => import('../pages/home/home')  // 路由懒加载
                },
                {
                    path: '/map',  // 地图
                    component: () => import('../pages/map/map')  // 路由懒加载
                },
                {
                    path: '/city/',  // 城市选择页
                    component: () => import('../pages/city/city')
                },
                {
                    path: '/msite', // 商品列表页（全部）
                    component: () => import('../pages/msite/msite'),
                    // 缓存组件
                    meta: { keepAlive: true },
                },
                {
                    path: '/search/:geohash',   // 搜索页
                    component: () => import('../pages/search/search')
                },
                {
                    path: '/food',   // 特色商铺页
                    component: () => import('../pages/food/food')
                },
                {
                    path: '/shop',   // 商铺页
                    component: () => import('../pages/shop/shop'),
                    meta: { keepAlive: false },
                    children: [
                        {
                            path: 'shopDetail', // 商铺详情页
                            component: () => import('../pages/shop/children/ShopDetail')
                        },
                        {
                            path: 'shopChat', // 联系商家
                            component: () => import('../pages/shop/shopChat/ShopChat')
                        }
                    ]
                },
                {
                    path: 'profile',   //  个人信息页
                    component: () => import('../pages/profile/profile'),
                    children: [{
                        path: 'info',   // 详情页
                        component: () => import('../pages/profile/children/info'),
                        children: [
                            {
                                path: 'setusername',
                                component: () => import('../pages/profile/children/children/setusername')
                            },
                            {
                                path: 'address',
                                component: () => import('../pages/profile/children/children/address'),
                                children: [{
                                    path: 'add',
                                    component: () => import('../pages/profile/children/children/children/add'),
                                    children: [{
                                        path: 'addDetail',
                                        component: () => import('../pages/profile/children/children/children/children/addDetail')
                                    }]
                                }]
                            }
                        ]
                    }]
                },
                {
                    path: 'login',  // 登录页
                    component: () => import('../pages/login/login')
                },
                {
                    path: 'register',  // 注册页
                    component: () => import('../pages/register/register')
                },
                {
                    path: 'forget', // 修改密码页
                    component: () => import('../pages/forget/forget')
                },
                {
                    path: 'order', // 订单
                    component: () => import('../pages/order/order'),
                    meta: {
                        keepAlive: false, //此组件不需要被缓存
                    },
                    children: [
                        {
                            path: 'payment',  // 支付
                            component: () => import('../pages/order/children/payment')
                        },
                        {
                            path: 'addcomment',  // 评论商品
                            component: () => import('../pages/order/children/addcomment')
                        }
                    ]
                },
                {
                    path: 'confirmOrder', // 订单
                    component: () => import('../pages/confirmOrder/confirmOrder'),
                    children: [
                        {
                            path: 'chooseAddress',  // 选择地址
                            component: () => import('../pages/confirmOrder/children/chooseAddress')
                        },
                        {
                            path: 'remark',  // 订单备注
                            component: () => import('../pages/confirmOrder/children/remark')
                        },
                        {
                            path: 'payment',  // 支付页
                            component: () => import('../pages/confirmOrder/children/payment')
                        },
                    ]
                },
            ]
        },
    ]
})

export default router