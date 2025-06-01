<template>
    <q-page class="window-height window-width row justify-center items-center" style="background: #f5f5f5;">
        <div class="column q-pa-lg">
            <div class="row">
                <q-card square bordered class="q-pa-lg shadow-1">
                    <q-card-section class="text-center">
                        <q-avatar size="80px" class="shadow-3">
                            <img src="https://cdn.quasar.dev/logo-v2/svg/logo-mono-primary.svg" />
                        </q-avatar>
                        <div class="text-h4 text-primary q-mt-md q-mb-sm">Create Account</div>
                        <div class="text-grey-8 q-mb-md">Join our community and start sharing moments</div>
                    </q-card-section>

                    <q-card-section>
                        <q-form @submit="onSubmit" @reset="onReset" class="q-gutter-md">
                            <div class="row q-col-gutter-md">
                                <div class="col-12 col-sm-6">
                                    <q-input filled v-model="user.firstName" label="First Name" class="register-input"
                                        lazy-rules
                                        :rules="[val => val && val.length > 0 || 'Please enter your first name']">
                                        <template v-slot:prepend>
                                            <q-icon name="person" color="primary" />
                                        </template>
                                    </q-input>
                                </div>

                                <div class="col-12 col-sm-6">
                                    <q-input filled v-model="user.lastName" label="Last Name" class="register-input"
                                        lazy-rules
                                        :rules="[val => val && val.length > 0 || 'Please enter your last name']">
                                        <template v-slot:prepend>
                                            <q-icon name="person" color="primary" />
                                        </template>
                                    </q-input>
                                </div>
                            </div>

                            <q-input filled v-model="user.email" type="email" label="Email" class="register-input"
                                lazy-rules :rules="[
                                    val => val && val.length > 0 || 'Please enter your email',
                                    val => val.includes('@') || 'Please enter a valid email'
                                ]">
                                <template v-slot:prepend>
                                    <q-icon name="email" color="primary" />
                                </template>
                            </q-input>

                            <q-input filled v-model="user.password" :type="isPwd ? 'password' : 'text'" label="Password"
                                class="register-input" lazy-rules :rules="[
                                    val => val && val.length > 0 || 'Please enter a password',
                                    val => val.length >= 8 || 'Password must be at least 8 characters'
                                ]">
                                <template v-slot:prepend>
                                    <q-icon name="lock" color="primary" />
                                </template>
                                <template v-slot:append>
                                    <q-icon :name="isPwd ? 'visibility_off' : 'visibility'" class="cursor-pointer"
                                        @click="isPwd = !isPwd" />
                                </template>
                            </q-input>

                            <q-input filled v-model="user.confirmPassword" :type="isPwd ? 'password' : 'text'"
                                label="Confirm Password" class="register-input" lazy-rules :rules="[
                                    val => val && val.length > 0 || 'Please confirm your password',
                                    val => val === user.password || 'Passwords do not match'
                                ]">
                                <template v-slot:prepend>
                                    <q-icon name="lock" color="primary" />
                                </template>
                            </q-input>

                            <div class="row items-center q-mt-md">
                                <q-checkbox v-model="acceptTerms"
                                    label="I accept the Terms of Service and Privacy Policy" color="primary" />
                            </div>

                            <div class="full-width q-pt-md">
                                <q-btn type="submit" color="primary" class="full-width" size="lg" :loading="isLoading"
                                    :disable="!acceptTerms">
                                    Sign Up
                                    <template v-slot:loading>
                                        <q-spinner-facebook />
                                    </template>
                                </q-btn>
                            </div>

                            <div class="row items-center justify-center q-mt-lg">
                                <q-separator class="col-4" />
                                <div class="col-auto q-px-sm text-grey-6">or sign up with</div>
                                <q-separator class="col-4" />
                            </div>

                            <div class="row items-center justify-center q-gutter-md">
                                <q-btn flat round color="blue-9">
                                    <q-icon name="fab fa-facebook" />
                                </q-btn>
                                <q-btn flat round color="red-5">
                                    <q-icon name="fab fa-google" />
                                </q-btn>
                                <q-btn flat round color="light-blue-5">
                                    <q-icon name="fab fa-twitter" />
                                </q-btn>
                            </div>
                        </q-form>
                    </q-card-section>

                    <q-card-section class="text-center q-pt-none">
                        <div class="text-grey-6">
                            Already have an account?
                            <router-link to="/auth/login" class="text-primary">Sign In</router-link>
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
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: ''
})

const isPwd = ref(true)
const acceptTerms = ref(false)
const isLoading = ref(false)

const onSubmit = async () => {
    if (!acceptTerms.value) {
        $q.notify({
            type: 'negative',
            message: 'You must accept the terms and conditions',
            position: 'top'
        })
        return
    }

    try {
        isLoading.value = true

        // Remove confirmPassword before sending to API
        const userData = {
            firstName: user.value.firstName,
            lastName: user.value.lastName,
            email: user.value.email,
            password: user.value.password
        }

        const response = await UserServices.register(userData)

        if (response.data) {
            $q.notify({
                type: 'positive',
                message: 'Registration successful! Please login.',
                position: 'top',
                timeout: 1500
            })

            router.push('/auth/login')
        }
    } catch (error) {
        console.error('Registration failed:', error)

        $q.notify({
            type: 'negative',
            message: error.response?.data?.message || 'Registration failed. Please try again.',
            position: 'top'
        })
    } finally {
        isLoading.value = false
    }
}

const onReset = () => {
    user.value = {
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        confirmPassword: ''
    }
    acceptTerms.value = false
}
</script>

<style lang="scss" scoped>
.register-input {
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
    max-width: 600px;
    border-radius: 16px;

    @media (max-width: 600px) {
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

@media (max-width: 599px) {
    .q-card {
        margin: 16px;
    }

    .q-form {
        .row>.col-12 {
            padding: 4px 0;
        }
    }
}
</style>
