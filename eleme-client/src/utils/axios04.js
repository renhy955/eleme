import Axios from 'axios'
import LocalStorage  from './localstorage'

// const baseURL = 'http://localhost:8080/boot'
// const baseURL = 'http://htuqn6.natappfree.cc/boot'
// const baseURL = 'https://elm.cangdu.org'
Axios.defaults.withCredentials = true;
const axios03 = Axios.create({
	baseURL:'https://restapi.amap.com/v3' 
})

axios03.defaults.withCredentials=true;
// axios.default.withCredentials = true    // 允许跨域携带cookie信息
// axios03.defaults.baseURL =  //关键代码
// 前置拦截器(请求前拦截)
axios03.interceptors.request.use(	
	(config)=>{
	config.headers = {
		"Content-Type":"application/x-www-form-urlencoded;charset=utf-8"
	}
	config.changeOrigin = true
	console.log("url",config.url)
	return config;

},
(error)=>{
	return Promise.reject(error)
},

)

// 后置拦截器(获取到响应后拦截)
axios03.interceptors.response.use(	
	(response)=>{
	let data = response.data;
	//只要前台被拦截的请求里面含这两个参数，那么就跳转到登录界面
	// if(!data.success && data.msg==="loginFail"){
	// 	location.href = "login.htaml";
	// }
	return response;
},
(error)=>{
	console.log(error);
	if(error.response.status === 400){

	}else if(error.response.status === 401){
		
	}else if(error.response.status === 403){
		
	}else if(error.response.status === 404){
		
	}else if(error.response.status === 500){
		
	}
	return Promise.resolve({});
}
)

export default axios03