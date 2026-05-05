import Axios from 'axios'
import LocalStorage  from './localstorage'

const baseURL = 'http://localhost:8080/boot'
// const baseURL = 'http://htuqn6.natappfree.cc/boot'
// const baseURL = 'https://elm.cangdu.org'
Axios.defaults.withCredentials = true;
const axios02 = Axios.create({
    baseURL,
})

axios02.defaults.withCredentials=true;
// axios.default.withCredentials = true    // 允许跨域携带cookie信息

// 前置拦截器(请求前拦截)
axios02.interceptors.request.use(	
	(config)=>{
	config.headers = {
		"Content-Type":"application/json;charset=UTF-8"
	}
	//防止爆红
	if (!config?.headers) {throw new Error(`Expected 'config' and 'config.headers' not to be undefined`);}
	//1.1 获取到浏览器里面一直存储的token，并将它放到
	// let uToken =  localStorage.getItem;
	let uToken = LocalStorage.getLocal("token");
	console.log("从浏览器本地存储里面获取到的token:"+uToken);

	if(uToken!=null){
		//1.2 注意：给请求头里面添加token（后台判断就是取的这个请求头）请求头，并把随机数的token值也设置进去
		config.headers['token']=uToken;
	}
	return config;

},
(error)=>{
	return Promise.reject(error)
},

)

// 后置拦截器(获取到响应后拦截)
axios02.interceptors.response.use(	
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

export default axios02