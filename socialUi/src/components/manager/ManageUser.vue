<template>
    <div class="manage-users">
        <div class="header-section q-mb-lg">
            <div class="text-h5">Manage Users</div>
            <div class="row q-gutter-sm">
                <q-input v-model="searchQuery" dense outlined placeholder="Search users..." class="search-input">
                    <template v-slot:append>
                        <q-icon name="search" />
                    </template>
                </q-input>
                <q-btn-group flat>
                    <q-btn icon="refresh" color="primary" @click="fetchUsers">
                        Refresh
                    </q-btn>
                    <q-btn icon="tune" color="primary">
                        Filters
                        <q-menu>
                            <q-list style="min-width: 150px">
                                <q-item tag="label" v-close-popup>
                                    <q-item-section>
                                        <q-item-label>Role</q-item-label>
                                        <q-select v-model="roleFilter" :options="['All', 'Admin', 'User']" dense
                                            options-dense class="q-mt-sm" />
                                    </q-item-section>
                                </q-item>
                                <q-item tag="label" v-close-popup>
                                    <q-item-section>
                                        <q-item-label>Status</q-item-label>
                                        <q-select v-model="statusFilter" :options="['All', 'Active', 'Inactive']" dense
                                            options-dense class="q-mt-sm" />
                                    </q-item-section>
                                </q-item>
                            </q-list>
                        </q-menu>
                    </q-btn>
                </q-btn-group>
            </div>
        </div>

        <div class="table-container">
            <q-table :rows="filteredUsers" :columns="columns" row-key="userId" :loading="loading" flat bordered
                separator="cell" class="users-table" v-model:pagination="pagination"
                :rows-per-page-options="[10, 20, 50]">
                <template v-slot:header="props">
                    <q-tr :props="props">
                        <q-th v-for="col in props.cols" :key="col.name" :props="props">
                            {{ col.label }}
                        </q-th>
                    </q-tr>
                </template>

                <template v-slot:body="props">
                    <q-tr :props="props" :class="{ 'inactive-user': !props.row.active }">
                        <q-td key="userId" :props="props">
                            {{ props.row.userId }}
                        </q-td>
                        <q-td key="avatar" :props="props">
                            <q-avatar>
                                <img :src="getRandomAvatar(props.row.userId)">
                            </q-avatar>
                        </q-td>
                        <q-td key="name" :props="props">
                            <div class="text-weight-medium">{{ getFullName(props.row) }}</div>
                            <div class="text-caption text-grey">{{ props.row.email }}</div>
                        </q-td>
                        <q-td key="role" :props="props">
                            <q-chip :color="props.row.role === 'ADMIN' ? 'purple' : 'primary'" text-color="white" dense
                                class="text-capitalize">
                                {{ props.row.role?.toLowerCase() || 'User' }}
                            </q-chip>
                        </q-td>
                        <q-td key="status" :props="props">
                            <q-chip :color="props.row.active ? 'positive' : 'grey'" text-color="white" dense>
                                {{ props.row.active ? 'Active' : 'Inactive' }}
                            </q-chip>
                        </q-td>
                        <q-td key="createdAt" :props="props">
                            {{ formatDate(props.row.createdAt) }}
                        </q-td>
                        <q-td key="actions" :props="props" class="text-center">
                            <div class="row q-gutter-sm justify-center">
                                <q-btn flat round size="sm" color="primary" icon="visibility"
                                    @click="viewUserDetails(props.row)">
                                    <q-tooltip>View Details</q-tooltip>
                                </q-btn>
                                <q-btn flat round size="sm" color="warning" icon="edit" @click="editUser(props.row)">
                                    <q-tooltip>Edit User</q-tooltip>
                                </q-btn>
                                <q-btn flat round size="sm" :color="props.row.active ? 'negative' : 'positive'"
                                    :icon="props.row.active ? 'block' : 'check_circle'"
                                    @click="toggleUserStatus(props.row)">
                                    <q-tooltip>{{ props.row.active ? 'Deactivate' : 'Activate' }} User</q-tooltip>
                                </q-btn>
                            </div>
                        </q-td>
                    </q-tr>
                </template>

                <template v-slot:loading>
                    <div class="text-center q-pa-md">
                        <q-spinner color="primary" size="2em" />
                        <div class="q-mt-sm">Loading users...</div>
                    </div>
                </template>

                <template v-slot:no-data>
                    <div class="text-center q-pa-lg">
                        <q-icon name="group_off" size="3em" color="grey-7" />
                        <div class="text-h6 q-mt-md">No users found</div>
                        <div class="text-grey">Try adjusting your search or filters</div>
                    </div>
                </template>
            </q-table>
        </div>

        <!-- User Details Dialog -->
        <q-dialog v-model="userDetailsDialog" class="user-details-dialog">
            <q-card style="width: 700px; max-width: 90vw;">
                <q-card-section class="row items-center">
                    <div class="text-h6">User Details</div>
                    <q-space />
                    <q-btn icon="close" flat round dense v-close-popup />
                </q-card-section>

                <q-separator />

                <q-card-section v-if="selectedUser" class="q-pa-lg">
                    <div class="row q-col-gutter-md">
                        <div class="col-12 col-sm-4 flex flex-center">
                            <q-avatar size="150px">
                                <img :src="getRandomAvatar(selectedUser.userId)">
                            </q-avatar>
                        </div>
                        <div class="col-12 col-sm-8">
                            <div class="text-h5 q-mb-md">{{ getFullName(selectedUser) }}</div>
                            <div class="row q-col-gutter-md">
                                <div class="col-12 col-sm-6">
                                    <div class="text-grey">Email</div>
                                    <div>{{ selectedUser.email }}</div>
                                </div>
                                <div class="col-12 col-sm-6">
                                    <div class="text-grey">User ID</div>
                                    <div>{{ selectedUser.userId }}</div>
                                </div>
                                <div class="col-12 col-sm-6">
                                    <div class="text-grey">Role</div>
                                    <div class="text-capitalize">{{ selectedUser.role?.toLowerCase() || 'User' }}</div>
                                </div>
                                <div class="col-12 col-sm-6">
                                    <div class="text-grey">Status</div>
                                    <div>{{ selectedUser.active ? 'Active' : 'Inactive' }}</div>
                                </div>
                                <div class="col-12 col-sm-6">
                                    <div class="text-grey">Created At</div>
                                    <div>{{ formatDate(selectedUser.createdAt) }}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </q-card-section>
            </q-card>
        </q-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { date } from 'quasar';
import { UserServices } from 'src/services/api';

const users = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const roleFilter = ref('All');
const statusFilter = ref('All');
const userDetailsDialog = ref(false);
const selectedUser = ref(null);

const pagination = ref({
    sortBy: 'createdAt',
    descending: true,
    page: 1,
    rowsPerPage: 10
});

const columns = [
    { name: 'userId', align: 'left', label: 'ID', field: 'userId', sortable: true },
    { name: 'avatar', align: 'center', label: 'Avatar', field: 'avatar' },
    { name: 'name', align: 'left', label: 'Name', field: row => getFullName(row), sortable: true },
    { name: 'role', align: 'center', label: 'Role', field: 'role', sortable: true },
    { name: 'status', align: 'center', label: 'Status', field: 'active' },
    { name: 'createdAt', align: 'left', label: 'Created At', field: 'createdAt', sortable: true },
    { name: 'actions', align: 'center', label: 'Actions' }
];

const filteredUsers = computed(() => {
    let result = [...users.value];

    // Apply search filter
    if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase();
        result = result.filter(user =>
            getFullName(user).toLowerCase().includes(query) ||
            user.email.toLowerCase().includes(query)
        );
    }

    // Apply role filter
    if (roleFilter.value !== 'All') {
        result = result.filter(user =>
            user.role?.toLowerCase() === roleFilter.value.toLowerCase()
        );
    }

    // Apply status filter
    if (statusFilter.value !== 'All') {
        const isActive = statusFilter.value === 'Active';
        result = result.filter(user => user.active === isActive);
    }

    return result;
});

const fetchUsers = async () => {
    loading.value = true;
    try {
        const response = await UserServices.getAllUsers();
        users.value = response.data;
    } catch (error) {
        console.error('Error fetching users:', error);
    } finally {
        loading.value = false;
    }
};

const getFullName = (user) => {
    return [user.firstName, user.surname, user.lastName]
        .filter(Boolean)
        .join(' ') || 'N/A';
};

const getRandomAvatar = (userId) => {
    const number = (userId % 8) + 1;
    return `https://cdn.quasar.dev/img/avatar${number}.jpg`;
};

const formatDate = (dateString) => {
    return date.formatDate(dateString, 'MMM D, YYYY');
};

const viewUserDetails = (user) => {
    selectedUser.value = user;
    userDetailsDialog.value = true;
};

const editUser = (user) => {
    // Implement edit functionality
    console.log('Edit user:', user);
};

const toggleUserStatus = async (user) => {
    try {
        // Implement status toggle functionality
        user.active = !user.active;
        console.log('Toggle user status:', user);
    } catch (error) {
        console.error('Error toggling user status:', error);
    }
};

// Fetch users on component mount
onMounted(() => {
    fetchUsers();
});
</script>

<style lang="scss" scoped>
.manage-users {
    padding: 20px;
}

.header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
}

.search-input {
    width: 300px;

    @media (max-width: 599px) {
        width: 100%;
    }
}

.table-container {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
    }
}

.users-table {
    .q-table__top {
        padding: 16px;
    }

    thead tr th {
        font-weight: 600;
        background: #f8f9fa;
    }

    tbody tr {
        transition: all 0.2s ease;

        &:hover {
            background: #f8f9fa;
        }

        &.inactive-user {
            background: #fafafa;
            opacity: 0.7;
        }

        td {
            padding: 12px 16px;
        }
    }
}

.user-details-dialog {
    .q-card {
        border-radius: 12px;
    }
}

@media (max-width: 599px) {
    .manage-users {
        padding: 12px;
    }

    .header-section {
        flex-direction: column;
        align-items: stretch;
    }

    .table-container {
        border-radius: 8px;
    }
}
</style>