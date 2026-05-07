import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import store from '../store'
import { getToken } from '@/utils/auth'
import LocalStorage from '@/utils/localstorage'
// axios.defaults.withCredentials=true
// 创建axios实例
const service = axios.create({
  // baseURL: process.env.BASE_API, // api的base_url
  baseURL: '/boot',
  withCredentials: true, // send cookies when cross-domain requests
  timeout: 10000
})
service.defaults.withCredentials=true;
// request拦截器
// service.interceptors.request.use(
//   config => {
//     var token = getToken()
//     if (token) {
//       config.headers['Authorization'] = token // 让每个请求携带自定义token 请根据实际情况自行修改
//     }
//     // config.headers.common['Content-Type'] = 'application/x-www-form-urlencoded'
//     // config.data = true
//     return config
//   },
//   error => {
//     // Do something with request error
//     console.log('error', error) // for debug
//     Promise.reject(error)
//   }
// )
service.interceptors.request.use(
  (config) => {
    config.headers = {
      "Content-Type": "application/json"
    }
    //防止爆红
    // if (!config?.headers) { throw new Error(`Expected 'config' and 'config.headers' not to be undefined`); }
    //1.1 获取到浏览器里面一直存储的token，并将它放到
    // let uToken =  localStorage.getItem;
    let uToken = LocalStorage.getLocal("token");
    console.log("从浏览器本地存储里面获取到的[request.js]token:" + uToken);

    if (uToken != null) {
      //1.2 注意：给请求头里面添加token（后台判断就是取的这个请求头）请求头，并把随机数的token值也设置进去
      config.headers['token'] = uToken;
    }
    return config;

  },
  (error) => {
    console.log('error', error) // for debug
    return Promise.reject(error)
  },

)
// respone拦截器
service.interceptors.response.use(
  response => {
    /**
     * code为非20000是抛错 可结合自己业务进行修改
     */
    const res = response.data
    if (res.errorCode !== 200) {
      // 50008:非法的token; 50012:其他客户端登录了;  50014:Token 过期了;
      if (res.errorCode === 50008 || res.errorCode === 50012 || res.errorCode === 50014) {
      }
      // return Promise.reject(res.errorMsg)
      return response
    } else {
      return response
    }
  },
  error => {
    //debug
    // if (error.response && error.response.data.errorCode) {
    //   Message({
    //     message: error.response.data.errorMsg,
    //     type: 'error',
    //     duration: 5 * 1000
    //   })
    // } else {
    //   Message({
    //     message: error.message,
    //     type: 'error',
    //     duration: 5 * 1000
    //   })
    // }
    console.log(error)//for debug
    return Promise.reject(error)
  }
)

export default service
