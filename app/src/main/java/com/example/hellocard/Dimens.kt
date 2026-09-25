package com.example.hellocard

/**
 * 全局尺寸 / 动画 / 游戏数值常量
 * 消除魔法数字，便于多屏适配与平衡调整
 */
object Dimens {

    // ========== 卡牌视图尺寸 ==========
    const val CARD_WIDTH_NORMAL = 140      // 场上/手牌卡牌宽度
    const val CARD_HEIGHT_NORMAL = 195     // 场上/手牌卡牌高度
    const val CARD_WIDTH_SMALL = 100       // 小尺寸（额外/场地）
    const val CARD_HEIGHT_SMALL = 140
    const val CARD_IMG_SIZE_NORMAL = 120   // 卡图区域
    const val CARD_IMG_SIZE_SMALL = 80
    const val CARD_MARGIN_END = 8          // 卡牌间距

    // ========== 区域占位框尺寸 ==========
    const val ZONE_MONSTER_W = 140
    const val ZONE_MONSTER_H = 195
    const val ZONE_SPELL_W = 50
    const val ZONE_SPELL_H = 55
    const val ZONE_SIDE_W = 70
    const val ZONE_SIDE_H = 55

    // ========== 卡组编辑卡片尺寸 ==========
    const val EDIT_TILE_WIDTH = 110
    const val EDIT_TILE_HEIGHT = 155
    const val EDIT_IMG_SIZE = 90

    // ========== 对话框图片尺寸 ==========
    const val DETAIL_IMG_SIZE = 400
    const val LIST_THUMB_SIZE = 72

    // ========== 圆角 ==========
    const val CARD_CORNER_RADIUS = 14f
    const val IMG_CORNER_RADIUS = 8f
    const val THUMB_CORNER_RADIUS = 6f

    // ========== 游戏数值 ==========
    const val STARTING_LIFE = 8000
    const val MAX_HAND_SIZE = 7
    const val MAX_FIELD_SIZE = 5
    const val MAX_SPELL_ZONE = 5
    const val INITIAL_DRAW_COUNT = 5
    const val MAIN_DECK_SIZE = 40
    const val EXTRA_DECK_SIZE = 15
    const val BATTLE_LOG_MAX = 100

    // ========== 星级祭品规则 ==========
    const val TRIBUTE_THRESHOLD_5_6 = 5    // 5-6 星需 1 祭品
    const val TRIBUTE_THRESHOLD_7 = 7      // 7+ 星需 2 祭品
    const val SPECIAL_SUMMON_TRIBUTES = 2  // 额外怪兽固定 2 祭品
    const val RESTORE_MAX_LEVEL = 4        // restore 复活上限

    // ========== 动画时长 (ms) ==========
    const val ANIM_FLIP_DURATION = 600L
    const val ANIM_ATTACK_DURATION = 400L
    const val ANIM_DAMAGE_SHAKE = 300L
    const val ANIM_PARTICLE_DEFAULT = 800L
    const val ANIM_CARD_IDLE_MIN = 1800L
    const val ANIM_CARD_IDLE_RANGE = 800L
    const val ANIM_AMBIENT_SPAWN_INTERVAL = 400L
    const val ANIM_AMBIENT_DURATION_MIN = 5000L
    const val ANIM_AMBIENT_DURATION_RANGE = 3000L

    // ========== AI 延迟 (ms) ==========
    const val AI_STEP_DELAY = 800L           // 玩家结束回合 → AI 开始
    const val AI_DELAY_ATTACK = 1700L
    const val AI_DELAY_EXTRA = 1900L
    const val AI_DELAY_SUMMON = 1300L
    const val AI_DELAY_DEFAULT = 1200L

    // ========== AI 决策 ==========
    const val AI_LOW_LIFE_THRESHOLD = 3000   // 低于此 LP 时倾向守备
    const val AI_DEFENSE_CHANCE = 0.4        // 守备表示概率

    // ========== 音效 ==========
    const val BGM_VOLUME = 0.4f
    const val SOUND_POOL_MAX_STREAMS = 8

    // ========== 粒子数量 ==========
    const val PARTICLE_SUMMON_COUNT = 120
    const val PARTICLE_ATTACK_COUNT = 80
    const val PARTICLE_DAMAGE_COUNT = 60
    const val PARTICLE_DESTROY_COUNT = 100
    const val PARTICLE_HEAL_COUNT = 80
    const val PARTICLE_AMBIENT_BATCH = 3
}
