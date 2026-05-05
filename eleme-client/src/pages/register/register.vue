<template>
    <div>
        <head-top head-title="注册" :go-back="true"></head-top>
        <section class="mt-10 bg-white">
            <!-- 账号密码验证码 -->
            <input type="text" placeholder="账号" v-model.lazy="userAccount" class="w-full p-2 border-b text-xxs">
            <div class="relative">
                <input :type="showPwd ? 'text' : 'password'" placeholder="密码" v-model.lazy="passWord" class="w-full p-2 border-b text-xxs">
                <div class="absolute top-1 right-2" @click="showPwd = !showPwd">
                    <van-icon v-if="showPwd" name="eye-o" />
                    <van-icon v-else name="closed-eye" />
                </div>
            </div>
            <div class="relative">
                <input :type="showPwd_two ? 'text' : 'password'" placeholder="再次输入密码" v-model.lazy="passWord_two" class="w-full p-2 border-b text-xxs">
				<div class="absolute top-1 right-2" @click="showPwd_two = !showPwd_two">
                    <van-icon v-if="showPwd_two" name="eye-o" />
                    <van-icon v-else name="closed-eye" />
                </div>
            </div>
				<input type="text" placeholder="邮箱" v-model.lazy="email" class="w-full p-2 border-b text-xxs">
                <input type="text" placeholder="验证码" v-model.lazy="yzcode" class="w-full p-2 border-b text-xxs"/>
        </section>
        <p class="text-3xs ml-2 my-2 text-red-500">请记住你的密码</p>
		<div class="px-2">
            <button :disabled="Rbtn!=='发送验证码'" class="w-full h-8 bg-green-400 rounded text-white" @click="sendCode">{{Rbtn}}</button>
        </div>
        <div class="h-2"></div>
        <div class="px-2">
            <button class="w-full h-8 bg-green-400 rounded text-white" @click="handleRegister">注册</button>
        </div>
        <van-dialog v-model:show="showAlert">
            <template #default>
                <div class="flex flex-col items-center">
                    <van-icon size="82" class="m-2 text-yellow-400" name="warning-o" />
                    <span class="mb-2">{{alertText}}</span>
                </div>
            </template>
            <template #footer>
                <div class="bg-green-400">
                    <button  class="w-full h-6 text-white" @click="showAlert=false">确认</button>
                </div>
            </template>
        </van-dialog>
    </div>
</template>

<script>
import { reactive, toRefs, onMounted } from 'vue'
import { login,getRegisterCode,register } from '@/api8080/login'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import HeadTop from '@/components/HeadTop'
import LocalStorage from '../../utils/localstorage'
import { Notify } from 'vant';
export default {
    components: {
        HeadTop
    },
    setup() {
		let email1_REG = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
        const store = useStore()
        const useMutation = store._mutations
        const router = useRouter()
        const state = reactive({
            showPwd: false,
			showPwd_two:false,
            showAlert: false,   // 是否显示提示框
            alertText: null,    // 提示文本
            userAccount: null,  //  用户名
            passWord: null, // 密码
			passWord_two:null,
			email:null,
            userInfo: null, // 用户信息
            Rbtn:'发送验证码',
            num:60,
            yzcode:null,
        })
        onMounted(() => {
            //拉取一次登录码
        })
        const settime = ()=>{
            if(state.num===0){
                state.Rbtn = '发送验证码';
                state.num = 60;
                return;
            }else{
                state.Rbtn = state.num + '秒后重新获取';
                state.num--;
            }
                setTimeout(function (){
                    settime();
                },1000)
        }
        //发送注册码
		const sendCode = async() => {
            console.log(email1_REG.test(state.email))
            if(state.email && email1_REG.test(state.email)){
                const resp = await getRegisterCode(state.email)
                const data = resp.data
                if(data.success){
                    Notify({ type: 'success', message: '验证码发送成功' });
                    settime();
                }else{
                    state.showAlert = true
                    state.alertText = data.errorMsg
                }
                
            }else if(!state.email){
                state.showAlert = true
                state.alertText = '请输入邮箱' 
            }else if(!email1_REG.test(state.email)){
                state.showAlert = true
                state.alertText = '邮箱格式不正确' 
            }
			

		}
		//处理注册
        const handleRegister = async() => {
            console.log("正在处理及注册请求");
            if(state.userAccount && state.passWord && state.passWord_two &&state.passWord===state.passWord_two
            && email1_REG.test(state.email) && state.yzcode && state.Rbtn != '发送验证码') {
				const resp = await register(
                    state.userAccount,
                    state.passWord,
                    state.passWord_two,
                    state.email,
                    state.yzcode,
                );
                const data = resp.data;
                if(data.success){
                    Notify({ type: 'success', message: '注册成功!正在跳转...' });
                    setTimeout(()=>{},1000)
                    router.go(-1)
                }else{
                    state.showAlert = true
                    state.alertText = data.errorMsg
                }
            }
            else if(!state.userAccount) {
                state.showAlert = true
                state.alertText = '请输入用户名'  
            }
            else if(!state.passWord) {
                state.showAlert = true
                state.alertText = '请输入密码'  
            }else if(!state.passWord_two){
				state.showAlert = true
                state.alertText = '请输入两次密码' 		
			}else if(state.passWord != state.passWord_two){
				state.showAlert = true
                state.alertText = '您两次输入的密码不一致'
			}else if(!state.email) {
                state.showAlert = true
                state.alertText = '邮箱未输入'
            }else if(!email1_REG.test(state.email)){
                state.showAlert = true
				state.alertText = '邮箱格式不正确'
			}else if(state.Rbtn == '发送验证码'){
                state.showAlert = true
                state.alertText = '您还没有发送验证码'
            }else if(!state.yzcode){
                state.showAlert = true
                state.alertText = '验证码不能为空'
            }else{
                state.showAlert = true
                state.alertText = '其他'
            }
        }
        return {
            ...toRefs(state),
            sendCode,
            handleRegister,
			sendCode
        }
    }
}
</script>

<style scoped>
</style>