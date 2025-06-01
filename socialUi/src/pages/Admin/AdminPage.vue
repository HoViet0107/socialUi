<template>
    <q-page class="q-pa-md">
        <q-tabs v-model="activeTab" class="text-primary" align="left" dense>
            <q-tab name="users" label="Manage Users" icon="people" />
            <q-tab name="posts" label="Manage Posts" icon="article" />
        </q-tabs>

        <q-separator />

        <div v-if="activeTab === 'users'">
            <ManageUser />
        </div>
        <div v-if="activeTab === 'posts'">
            <ManagePost />
        </div>
    </q-page>
</template>

<script setup>
import ManagePost from "src/components/manager/ManageFeedItem.vue";
import ManageUser from "src/components/manager/ManageUser.vue";
import { ref } from "vue";

const activeTab = ref("users"); // Default tab
</script>

<style scoped>
.q-page {
    max-width: 1200px;
    margin: 0 auto;
}
</style>