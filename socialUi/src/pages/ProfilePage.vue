<template>
  <q-page class="bg-grey-2">
    <div class="profile-container">
      <!-- Cover photo and profile picture -->
      <div class="profile-header">
        <div class="cover-photo">
          <img src="https://cdn.quasar.dev/img/mountains.jpg" alt="Cover photo">
          <div class="cover-overlay"></div>

          <div class="profile-picture">
            <q-avatar size="160px">
              <img src="https://cdn.quasar.dev/img/boy-avatar.png">
              <q-btn round color="primary" class="absolute-bottom-right" icon="photo_camera" size="sm">
                <q-tooltip>Update profile photo</q-tooltip>
              </q-btn>
            </q-avatar>
          </div>
        </div>

        <div class="profile-actions">
          <q-btn color="primary" icon="edit" label="Edit Profile" @click="openEditDialog" />
          <q-btn unelevated color="primary" icon="person_add" label="Add Friend" />
          <q-btn flat color="grey" icon="more_horiz">
            <q-menu>
              <q-list style="min-width: 150px">
                <q-item clickable v-close-popup @click="shareProfile">
                  <q-item-section avatar>
                    <q-icon name="share" />
                  </q-item-section>
                  <q-item-section>Share Profile</q-item-section>
                </q-item>
                <q-item clickable v-close-popup @click="reportProfile">
                  <q-item-section avatar>
                    <q-icon name="flag" />
                  </q-item-section>
                  <q-item-section>Report Profile</q-item-section>
                </q-item>
                <q-separator />
                <q-item clickable v-close-popup class="text-negative" @click="blockUser">
                  <q-item-section avatar>
                    <q-icon name="block" />
                  </q-item-section>
                  <q-item-section>Block User</q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
        </div>
      </div>

      <!-- Profile information -->
      <div class="profile-content q-px-md">
        <!-- Loading state -->
        <div v-if="loading" class="text-center q-pa-lg">
          <q-spinner color="primary" size="3em" />
          <div class="q-mt-md">Loading profile...</div>
        </div>

        <template v-else-if="user">
          <!-- Basic info card -->
          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-8">
              <!-- Profile Overview -->
              <q-card flat bordered class="profile-card q-mb-md">
                <q-card-section>
                  <div class="text-h4 q-mb-sm">{{ fullName }}</div>
                  <div class="text-subtitle1 text-grey-8">{{ user.headline || 'No headline added yet' }}</div>
                  <div class="text-body2 q-mt-md">{{ user.bio || 'No bio added yet' }}</div>

                  <q-separator class="q-my-md" />

                  <div class="row q-col-gutter-md">
                    <div class="col-12 col-sm-6">
                      <div class="info-item">
                        <q-icon name="email" color="primary" size="sm" />
                        <div>
                          <div class="info-label">Email</div>
                          <div class="info-value">{{ user.email || 'Not provided' }}</div>
                        </div>
                      </div>
                    </div>

                    <div class="col-12 col-sm-6">
                      <div class="info-item">
                        <q-icon name="phone" color="primary" size="sm" />
                        <div>
                          <div class="info-label">Phone</div>
                          <div class="info-value">{{ user.phone || 'Not provided' }}</div>
                        </div>
                      </div>
                    </div>

                    <div class="col-12 col-sm-6">
                      <div class="info-item">
                        <q-icon name="cake" color="primary" size="sm" />
                        <div>
                          <div class="info-label">Date of Birth</div>
                          <div class="info-value">{{ formatDate(user.dob) || 'Not provided' }}</div>
                        </div>
                      </div>
                    </div>

                    <div class="col-12 col-sm-6">
                      <div class="info-item">
                        <q-icon name="location_on" color="primary" size="sm" />
                        <div>
                          <div class="info-label">Location</div>
                          <div class="info-value">{{ user.location || 'Not provided' }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </q-card-section>
              </q-card>

              <!-- Recent Posts -->
              <q-card flat bordered class="profile-card">
                <q-card-section>
                  <div class="text-h6 q-mb-md">Recent Posts</div>
                  <template v-if="user.recentPosts && user.recentPosts.length > 0">
                    <div v-for="post in user.recentPosts" :key="post.id" class="post-item q-mb-md">
                      <div class="text-caption text-grey">{{ formatDate(post.date) }}</div>
                      <div class="text-body1 q-mt-sm">{{ post.content }}</div>
                      <div class="row items-center q-gutter-x-md q-mt-sm">
                        <q-btn flat dense size="sm" icon="favorite" :label="post.likes || 0" />
                        <q-btn flat dense size="sm" icon="mode_comment" :label="post.comments || 0" />
                      </div>
                      <q-separator class="q-mt-md" />
                    </div>
                  </template>
                  <div v-else class="text-center text-grey q-pa-lg">
                    No posts to display
                  </div>
                </q-card-section>
              </q-card>
            </div>

            <div class="col-12 col-md-4">
              <!-- Stats Card -->
              <q-card flat bordered class="profile-card q-mb-md">
                <q-card-section>
                  <div class="text-h6 q-mb-md">Activity</div>
                  <div class="row q-col-gutter-md">
                    <div class="col-4">
                      <div class="stat-item">
                        <div class="stat-value">{{ user.stats?.posts || 0 }}</div>
                        <div class="stat-label">Posts</div>
                      </div>
                    </div>

                    <div class="col-4">
                      <div class="stat-item">
                        <div class="stat-value">{{ user.stats?.friends || 0 }}</div>
                        <div class="stat-label">Friends</div>
                      </div>
                    </div>

                    <div class="col-4">
                      <div class="stat-item">
                        <div class="stat-value">{{ user.stats?.groups || 0 }}</div>
                        <div class="stat-label">Groups</div>
                      </div>
                    </div>
                  </div>
                </q-card-section>
              </q-card>

              <!-- Friends Preview -->
              <q-card flat bordered class="profile-card">
                <q-card-section>
                  <div class="row items-center justify-between q-mb-md">
                    <div class="text-h6">Friends</div>
                    <q-btn flat color="primary" label="See All" />
                  </div>

                  <div class="row q-col-gutter-sm">
                    <div v-for="i in 6" :key="i" class="col-4">
                      <q-avatar size="60px" class="cursor-pointer">
                        <img :src="`https://cdn.quasar.dev/img/avatar${i}.jpg`">
                        <q-tooltip>Friend Name</q-tooltip>
                      </q-avatar>
                    </div>
                  </div>
                </q-card-section>
              </q-card>
            </div>
          </div>
        </template>

        <div v-else class="text-center q-pa-lg">
          <q-icon name="error_outline" size="4em" color="grey-7" />
          <div class="text-h6 q-mt-md">User not found</div>
          <div class="text-grey">The requested profile could not be loaded</div>
        </div>
      </div>
    </div>

    <EditProfileDialog v-model="isEditDialogOpen" :user="user" @save="updateUserProfile" />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { format } from 'date-fns';
import { UserServices } from 'src/services/api';
import EditProfileDialog from 'src/components/editor/EditProfileDialog.vue';

const user = ref(null);
const loading = ref(true);
const isEditDialogOpen = ref(false);

// Computed property for full name
const fullName = computed(() => {
  if (!user.value) return '';
  return [
    user.value.firstName || '',
    user.value.surname || '',
    user.value.lastName || ''
  ].filter(Boolean).join(' ');
});

// Helper function to get auth token
const getAuthToken = () => {
  try {
    return JSON.parse(localStorage.getItem("authUser"));
  } catch (error) {
    console.error("Error parsing authUser:", error);
    return null;
  }
};

// Format date for display
function formatDate(dateString) {
  if (!dateString) return '';
  try {
    return format(new Date(dateString), 'MMMM dd, yyyy');
  } catch (e) {
    console.error('Error formatting date:', e);
    return dateString;
  }
}

// Get current user data
async function fetchUserData() {
  try {
    loading.value = true;
    // Get user details from token or another API call
    const response = await UserServices.getUserByEmail(getAuthToken());
    user.value = response.data;

    // Add placeholder data for testing if needed
    if (!user.value.stats) {
      user.value.stats = {
        posts: 12,
        friends: 45,
        groups: 3
      };
    }

    if (!user.value.recentPosts) {
      user.value.recentPosts = [
        {
          id: 1,
          date: new Date(),
          content: 'This is a sample post to demonstrate the UI.'
        }
      ];
    }
  } catch (error) {
    console.error('Error fetching user data:', error);
  } finally {
    loading.value = false;
  }
}

// Open edit dialog
const openEditDialog = () => {
  isEditDialogOpen.value = true;
};

// Handle profile update
const updateUserProfile = (updatedUser) => {
  // In a real app, you would send this to the server
  console.log('Profile updated:', updatedUser);

  // Update local user data
  user.value = {
    ...user.value,
    ...updatedUser
  };
};

// Share profile
const shareProfile = () => {
  // Implement share functionality
  console.log('Share profile');
}

// Report profile
const reportProfile = () => {
  // Implement report functionality
  console.log('Report profile');
}

// Block user
const blockUser = () => {
  // Implement block functionality
  console.log('Block user');
}

// Fetch user data on component mount
onMounted(fetchUserData);
</script>

<style lang="scss">
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
}

.profile-header {
  margin-bottom: 80px;
  position: relative;

  .cover-photo {
    height: 300px;
    overflow: hidden;
    position: relative;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .cover-overlay {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 50%;
      background: linear-gradient(to bottom, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.4));
    }
  }

  .profile-picture {
    position: absolute;
    bottom: -80px;
    left: 32px;

    .q-avatar {
      border: 4px solid white;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
    }
  }

  .profile-actions {
    position: absolute;
    bottom: -60px;
    right: 32px;
    display: flex;
    gap: 12px;

    @media (max-width: 600px) {
      position: relative;
      bottom: auto;
      right: auto;
      justify-content: center;
      padding: 16px;
      margin-top: 60px;
    }
  }
}

.profile-content {
  padding-bottom: 40px;
}

.profile-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.03);
  }

  .q-icon {
    margin-top: 4px;
  }

  .info-label {
    font-size: 0.8rem;
    color: $grey-7;
    margin-bottom: 4px;
  }

  .info-value {
    font-weight: 500;
  }
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: $grey-2;
  border-radius: 12px;
  transition: all 0.3s ease;

  &:hover {
    background: $grey-3;
    transform: translateY(-2px);
  }

  .stat-value {
    font-size: 1.5rem;
    font-weight: bold;
    color: $primary;
  }

  .stat-label {
    font-size: 0.9rem;
    color: $grey-7;
    margin-top: 4px;
  }
}

.post-item {
  padding: 16px 0;

  &:hover {
    .q-btn {
      opacity: 1;
    }
  }

  .q-btn {
    opacity: 0.7;
    transition: all 0.3s ease;

    &:hover {
      opacity: 1;
      transform: translateY(-1px);
    }
  }
}

@media (max-width: 600px) {
  .profile-header {
    margin-bottom: 120px;

    .cover-photo {
      height: 200px;
    }

    .profile-picture {
      left: 50%;
      transform: translateX(-50%);

      .q-avatar {
        width: 120px;
        height: 120px;
      }
    }
  }

  .stat-item {
    padding: 12px;

    .stat-value {
      font-size: 1.2rem;
    }

    .stat-label {
      font-size: 0.8rem;
    }
  }
}
</style>