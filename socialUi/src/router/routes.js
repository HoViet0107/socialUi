const routes = [
  // auth router
  {
    path: '/',
    component: () => import('src/layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('src/pages/IndexPage.vue') },
      { path: 'group', component: () => import('src/pages/GroupPage.vue') },
      { path: 'chat', component: () => import('src/pages/Chat/ChatsPage.vue') },
      { path: 'chat/:conversationId', component: () => import('src/pages/Chat/ChatsPage.vue') },
      { path: 'admin/admin-page', component: () => import('src/pages/Admin/AdminPage.vue') },
      { path: 'user/profile', component: () => import('src/pages/ProfilePage.vue') },
      {
        path: 'auth',
        children: [
          { path: 'login', component: () => import('src/pages/Auth/LoginPage.vue') },
          { path: 'register', component: () => import('src/pages/Auth/RegisterPage.vue') }
        ]
      }
    ]
  },
  //
  {
    path: '/:catchAll(.*)*',
    component: () => import('src/pages/ErrorNotFound.vue'),
  },
];

export default routes;
