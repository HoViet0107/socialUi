<template>
    <q-page class="window-height window-width row justify-center items-center" style="background: #f5f5f5;">
        <div class="column q-pa-lg">
            <div class="row">
                <q-card square bordered class="q-pa-lg shadow-1">
                    <q-card-section class="text-center">
                        <q-avatar size="80px" class="shadow-3">
                            <img src="https://cdn.quasar.dev/logo-v2/svg/logo-mono-primary.svg" />
                        </q-avatar>
                        <div class="text-h4 text-primary q-mt-md q-mb-sm">Welcome Back!</div>
                        <div class="text-grey-8 q-mb-md">Login to connect with friends and share moments</div>
                    </q-card-section>

                    <q-card-section>
                        <q-form @submit="onSubmit" class="q-gutter-md">
                            <!-- <q-input filled v-model="user.email" type="email" label="Email" class="login-input"
                                lazy-rules :rules="[
                                    val => val && val.length > 0 || 'Please enter your email',
                                    val => val.includes('@') || 'Please enter a valid email'
                                ]">
                                <template v-slot:prepend>
                                    <q-icon name="email" color="primary" />
                                </template>
</q-input> -->

                            <q-input filled v-model="user.email" type="email" label="Email" class="login-input"
                                lazy-rules :rules="[
                                    val => val && val.length > 0 || 'Please enter your email'
                                ]">
                                <template v-slot:prepend>
                                    <q-icon name="email" color="primary" />
                                </template>
                            </q-input>

                            <q-input filled v-model="user.password" :type="isPwd ? 'password' : 'text'" label="Password"
                                class="login-input" lazy-rules
                                :rules="[val => val && val.length > 0 || 'Please enter your password']">
                                <template v-slot:prepend>
                                    <q-icon name="lock" color="primary" />
                                </template>
                                <template v-slot:append>
                                    <q-icon :name="isPwd ? 'visibility_off' : 'visibility'" class="cursor-pointer"
                                        @click="isPwd = !isPwd" />
                                </template>
                            </q-input>

                            <div class="row items-center justify-between q-mt-md">
                                <q-checkbox dense v-model="rememberMe" label="Remember me" color="primary" />
                                <q-btn flat color="primary" label="Forgot Password?" type="button" />
                            </div>

                            <div class="full-width q-pt-md">
                                <q-btn type="submit" color="primary" class="full-width" size="lg" :loading="isLoading">
                                    Sign In
                                    <template v-slot:loading>
                                        <q-spinner-facebook />
                                    </template>
                                </q-btn>
                            </div>

                            <div class="row items-center justify-center q-mt-lg q-gutter-md">
                                <q-btn flat round color="grey-7">
                                    <q-icon name="fab fa-facebook" />
                                </q-btn>
                                <q-btn flat round color="grey-7">
                                    <q-icon name="fab fa-google" />
                                </q-btn>
                                <q-btn flat round color="grey-7">
                                    <q-icon name="fab fa-twitter" />
                                </q-btn>
                            </div>
                        </q-form>
                    </q-card-section>

                    <q-card-section class="text-center q-pt-none">
                        <div class="text-grey-6">
                            Don't have an account?
                            <router-link to="/auth/register" class="text-primary">Sign Up</router-link>
                        </div>
                    </q-card-section>
                </q-card>
            </div>
        </div>
    </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { UserServices } from 'src/services/api'

const $q = useQuasar()
const router = useRouter()

const user = ref({
    email: '',
    password: ''
})

const isPwd = ref(true)
const rememberMe = ref(false)
const isLoading = ref(false)

const onSubmit = async () => {
    try {
        isLoading.value = true
        const response = await UserServices.login(user.value)

        if (response.data) {
            localStorage.setItem('authUser', JSON.stringify(response.data.token))

            $q.notify({
                type: 'positive',
                message: 'Login successful!',
                position: 'top',
                timeout: 1500
            })

            // Remember user if checkbox is checked
            if (rememberMe.value) {
                localStorage.setItem('rememberedUser', JSON.stringify({
                    email: user.value.email,
                    timestamp: new Date().getTime()
                }))
            }

            // Redirect to home page
            router.push('/')
        }
    } catch (error) {
        console.error('Login failed:', error)

        $q.notify({
            type: 'negative',
            message: error.response?.data?.message || 'Login failed. Please try again.',
            position: 'top',
            timeout: 2000
        })
    } finally {
        isLoading.value = false
    }
}
</script>

<style lang="scss" scoped>
.login-input {
    .q-field__control {
        height: 56px;
        border-radius: 8px;
    }

    .q-field__marginal {
        height: 56px;
    }
}

.q-card {
    width: 100%;
    max-width: 400px;
    border-radius: 16px;

    @media (max-width: 400px) {
        width: 90%;
    }
}

:deep(.q-btn) {
    border-radius: 8px;
    padding: 8px 24px;
}

a {
    text-decoration: none;
    font-weight: 500;

    &:hover {
        text-decoration: underline;
    }
}
</style>
