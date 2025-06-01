<template>
    <q-dialog v-model="dialogVisible" persistent>
        <q-card style="min-width: 350px">
            <q-card-section>
                <div class="text-h6">Edit Profile</div>
            </q-card-section>

            <q-card-section>
                <q-form @submit="onSubmit" class="q-gutter-md">
                    <div class="row q-col-gutter-md">
                        <div class="col-12 col-md-4">
                            <q-input v-model="form.firstName" label="First Name" outlined dense />
                        </div>
                        <div class="col-12 col-md-4">
                            <q-input v-model="form.surname" label="Surname" outlined dense />
                        </div>
                        <div class="col-12 col-md-4">
                            <q-input v-model="form.lastName" label="Last Name" outlined dense />
                        </div>
                    </div>

                    <q-input v-model="form.phone" label="Phone Number" outlined dense />

                    <q-input v-model="form.dob" label="Date of Birth" outlined dense type="date" />

                    <div class="text-subtitle2 q-mt-md">Change Password (optional)</div>
                    <q-input v-model="form.password" label="New Password" outlined dense type="password"
                        hint="Leave blank to keep current password" />

                    <div class="row justify-end q-mt-md">
                        <q-btn label="Cancel" color="grey" flat v-close-popup />
                        <q-btn label="Save" type="submit" color="primary" :loading="loading" />
                    </div>
                </q-form>
            </q-card-section>
        </q-card>
    </q-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import { UserServices } from 'src/services/api';
import { useToast } from "vue-toastification";

const toast = useToast();

const props = defineProps({
    modelValue: {
        type: Boolean,
        default: false
    },
    userData: {
        type: Object,
        required: false
    }
});

const emit = defineEmits(['update:modelValue', 'saved']);
const loading = ref(false);

// Form data
const form = ref({
    userId: null,
    firstName: '',
    surname: '',
    lastName: '',
    phone: '',
    dob: '',
    password: ''
});

// Computed property for dialog visibility
const dialogVisible = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value)
});

// Watch for changes in userData to update form
watch(() => props.userData, (newValue) => {
    if (newValue) {
        form.value = {
            userId: newValue.userId,
            firstName: newValue.firstName || '',
            surname: newValue.surname || '',
            lastName: newValue.lastName || '',
            phone: newValue.phone || '',
            dob: newValue.dob ? formatDateForInput(newValue.dob) : '',
            password: '' // Always empty for security
        };
    }
}, { immediate: true });

// Format date for input field (YYYY-MM-DD)
function formatDateForInput(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toISOString().split('T')[0];
}

// Submit form
async function onSubmit() {
    try {
        loading.value = true;

        // Get token from localStorage
        const token = JSON.parse(localStorage.getItem('authUser'));
        if (!token) {
            toast.error('You must be logged in to update your profile');
            return;
        }

        // Create data object, removing empty password if not changed
        const userData = { ...form.value };
        if (!userData.password) {
            delete userData.password;
        }

        // Call API to update user profile
        const response = await UserServices.editUser(userData, token);
        
        toast.success('Profile updated successfully');

        // Emit event to parent component
        emit('saved', response.data);
        dialogVisible.value = false;
    } catch (error) {
        console.error('Error updating profile:', error);
        toast.error(error.response?.data || 'Failed to update profile');
    } finally {
        loading.value = false;
    }
}
</script>