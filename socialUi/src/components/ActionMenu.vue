<template>
    <div class="absolute-top-right">
        <q-btn flat round dense icon="more_vert">
            <q-menu fit anchor="bottom right" self="top right">
                <q-list dense>
                    <q-item clickable v-close-popup @click="edit(props.item, props.item.type)">
                        <q-item-section avatar>
                            <q-icon name="edit" />
                        </q-item-section>
                        <q-item-section>Edit</q-item-section>
                    </q-item>

                    <q-item clickable v-close-popup @click="remove(props.item)">
                        <q-item-section avatar>
                            <q-icon name="delete" />
                        </q-item-section>
                        <q-item-section>Delete</q-item-section>
                    </q-item>

                    <q-item clickable v-close-popup @click="report">
                        <q-item-section avatar>
                            <q-icon name="flag" />
                        </q-item-section>
                        <q-item-section>Báo cáo</q-item-section>
                    </q-item>
                </q-list>
            </q-menu>
        </q-btn>
    </div>
</template>

<script setup>
const props = defineProps({
    item: {
        type: Object,
        required: true
    }
});

const emit = defineEmits(["edit", "remove", "report"]);

const edit = (item, type) => {
    emit("edit", { item, type });
};

const remove = (item) => {
    emit("remove", item);
};

const report = () => {
    emit("report");
};
</script>
