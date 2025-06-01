<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated class="header-shadow">
      <q-toolbar class="glossy">
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="leftDrawerOpen = !leftDrawerOpen" />

        <q-toolbar-title class="text-weight-bold cursor-pointer" @click="$router.push('/')">
          <div class="row items-center">
            <q-avatar size="32px" class="q-mr-sm shadow-3">
              <img src="https://cdn.quasar.dev/logo-v2/svg/logo-mono-white.svg">
            </q-avatar>
            <span class="text-h6">Social App</span>
          </div>
        </q-toolbar-title>

        <div class="q-gutter-md row items-center no-wrap">
          <q-btn round flat icon="notifications" to="/notifications" class="header-btn">
            <q-badge color="red" floating rounded>2</q-badge>
          </q-btn>
          <q-btn round flat icon="message" to="/chat" class="header-btn">
            <q-badge color="red" floating rounded>4</q-badge>
          </q-btn>
          <q-btn round flat class="header-btn" @click="toggleRightDrawer">
            <q-avatar size="32px">
              <img src="https://cdn.quasar.dev/img/boy-avatar.png">
            </q-avatar>
          </q-btn>
        </div>
      </q-toolbar>
    </q-header>

    <!-- right drawer -->
    <q-drawer v-model="rightDrawerOpen" side="right" bordered content-class="bg-grey-1">
      <q-list padding>
        <q-item-label header class="text-grey-8">User Menu</q-item-label>

        <q-item clickable v-ripple to="/user/profile">
          <q-item-section avatar>
            <q-icon name="account_circle" color="primary" />
          </q-item-section>
          <q-item-section>Profile</q-item-section>
        </q-item>

        <!-- Thêm mục Groups vào sidebar -->
        <q-item clickable v-ripple to="/groups">
          <q-item-section avatar>
            <q-icon name="group" color="teal" />
          </q-item-section>
          <q-item-section>Groups</q-item-section>
        </q-item>

        <q-item clickable v-ripple v-show="isAdminUser()" to="/admin/admin-page">
          <q-item-section avatar>
            <q-icon name="admin_panel_settings" color="deep-orange" />
          </q-item-section>
          <q-item-section>Admin Panel</q-item-section>
        </q-item>

        <q-item clickable v-ripple v-if="isTokenExpired()" to="/auth/login">
          <q-item-section avatar>
            <q-icon name="login" color="green" />
          </q-item-section>
          <q-item-section>Login</q-item-section>
        </q-item>

        <q-item clickable v-ripple v-else @click="logout">
          <q-item-section avatar>
            <q-icon name="logout" color="negative" />
          </q-item-section>
          <q-item-section>Logout</q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container class="bg-grey-2">
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue';
import { isAdminUser, isTokenExpired } from 'src/helpers/helperFunctions';

const rightDrawerOpen = ref(false);
const leftDrawerOpen = ref(false);

const toggleRightDrawer = () => {
  rightDrawerOpen.value = !rightDrawerOpen.value;
};

// Add logout function
const logout = () => {
  localStorage.removeItem('token');
  window.location.href = '/auth/login';
};
</script>

<style lang="scss">
@use 'sass:color';

.q-header {
  .q-toolbar {
    height: 64px;
    background: linear-gradient(135deg, var(--q-primary) 0%, color.adjust($primary, $lightness: 15%) 100%);
  }
}

.header-shadow {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.header-btn {
  transition: transform 0.3s ease;

  &:hover {
    transform: translateY(-2px);
  }
}

.cursor-pointer {
  cursor: pointer;
}

.q-drawer {
  background: #ffffff;
  border-right: 1px solid #e0e0e0;

  .q-item {
    border-radius: 8px;
    margin: 8px;
    transition: all 0.3s ease;

    &:hover {
      background: #f5f5f5;
    }

    &.q-router-link--active {
      background: linear-gradient(135deg, var(--q-primary) 0%, color.adjust($primary, $lightness: 15%) 100%);
      ;
      color: white;
      font-weight: 500;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

      .q-icon {
        color: white !important;
      }
    }
  }
}

.q-page-container {
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* Responsive styles */
@media (max-width: 599px) {
  .q-toolbar-title {
    font-size: 1.1rem;
  }

  .header-btn {
    padding: 4px;
  }

  .q-drawer {
    .q-item {
      margin: 4px;
    }
  }
}
</style>