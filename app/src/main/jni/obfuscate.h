// Created by ridhoae303
// https://github.com/ridhoae303

#ifndef AY_OBFUSCATE_H
#define AY_OBFUSCATE_H

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <chrono>
#include <mutex>
#include <random>
#include <utility>

#if defined(_WIN32)
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#endif

namespace ay {

using size_type = std::size_t;

namespace runtime {

struct runtime_state {
    std::atomic<std::uint64_t> dynamic_key;
    std::once_flag init_flag;

    runtime_state() : dynamic_key(0), init_flag() {}
};

inline runtime_state& state() {
    static runtime_state instance;
    return instance;
}

inline std::uint64_t mix_seed(std::uint64_t value) {
    value ^= value >> 33;
    value *= 0xff51afd7ed558ccdULL;
    value ^= value >> 33;
    value *= 0xc4ceb9fe1a85ec53ULL;
    value ^= value >> 33;
    return value | 0x0101010101010101ULL;
}

inline bool has_tracer() {
#if defined(__ANDROID__) || defined(__linux__)
    FILE* status = std::fopen("/proc/self/status", "r");
    if (status == nullptr) {
        return false;
    }

    bool detected = false;
    char line[256];
    while (std::fgets(line, sizeof(line), status) != nullptr) {
        if (std::strncmp(line, "TracerPid:", 10) == 0) {
            detected = std::strtol(line + 10, nullptr, 10) > 0;
            break;
        }
    }
    std::fclose(status);
    return detected;
#elif defined(_WIN32)
    return ::IsDebuggerPresent() != 0;
#else
    return false;
#endif
}

#ifndef AY_OBFUSCATE_ABORT_ON_DEBUGGER
#if defined(NDEBUG)
#define AY_OBFUSCATE_ABORT_ON_DEBUGGER 1
#else
#define AY_OBFUSCATE_ABORT_ON_DEBUGGER 0
#endif
#endif

inline void check_debugger() {
#if AY_OBFUSCATE_ABORT_ON_DEBUGGER
    if (has_tracer()) {
        std::abort();
    }
#endif
}

inline void initialize() {
    runtime_state& runtime = state();
    std::call_once(runtime.init_flag, [&runtime]() {
        check_debugger();

        const std::uint64_t steady_ticks = static_cast<std::uint64_t>(
                std::chrono::steady_clock::now().time_since_epoch().count());
        const std::uint64_t system_ticks = static_cast<std::uint64_t>(
                std::chrono::system_clock::now().time_since_epoch().count());
        std::uint64_t seed = steady_ticks ^ (system_ticks << 1) ^
                static_cast<std::uint64_t>(reinterpret_cast<std::uintptr_t>(&runtime)) ^
                0x9e3779b97f4a7c15ULL;

        try {
            std::random_device random_source;
            for (unsigned int i = 0; i < 4; ++i) {
                const std::uint64_t random_value =
                        static_cast<std::uint64_t>(random_source());
                seed ^= random_value << ((i & 1u) * 32u);
                seed = mix_seed(seed + static_cast<std::uint64_t>(i));
            }
        } catch (...) {
        }

        seed = mix_seed(seed);
        if (seed == 0) {
            seed = 0xa5a5a5a55a5a5a5aULL;
        }
        runtime.dynamic_key.store(seed, std::memory_order_release);
    });
}

inline std::uint64_t get_dynamic_key() {
    initialize();
    return state().dynamic_key.load(std::memory_order_acquire);
}

}

namespace detail {

constexpr std::uint32_t rotate_left32(std::uint32_t value, unsigned rotation) {
    return static_cast<std::uint32_t>((value << (rotation & 31)) |
            (value >> ((32 - rotation) & 31)));
}

constexpr std::uint8_t rotate_left8(std::uint8_t value, unsigned rotation) {
    return static_cast<std::uint8_t>((value << (rotation & 7)) |
            (value >> ((8 - rotation) & 7)));
}

constexpr std::uint8_t rotate_right8(std::uint8_t value, unsigned rotation) {
    return static_cast<std::uint8_t>((value >> (rotation & 7)) |
            (value << ((8 - rotation) & 7)));
}

constexpr std::uint64_t mix64(std::uint64_t value) {
    value += 0x9e3779b97f4a7c15ULL;
    value = (value ^ (value >> 30)) * 0xbf58476d1ce4e5b9ULL;
    value = (value ^ (value >> 27)) * 0x94d049bb133111ebULL;
    return value ^ (value >> 31);
}

constexpr std::uint32_t fnv1a(const char* value, std::uint32_t hash = 2166136261u) {
    return (*value) ? fnv1a(value + 1, (hash ^ static_cast<std::uint32_t>(*value)) * 16777619u) : hash;
}

struct key128 {
    std::uint64_t a;
    std::uint64_t b;
};

constexpr key128 make_key(int line, int counter, const char* file) {
    const std::uint64_t seed1 = static_cast<std::uint64_t>(line) ^
            (static_cast<std::uint64_t>(counter) << 23);
    const std::uint64_t seed2 = static_cast<std::uint64_t>(fnv1a(file));
    const std::uint64_t part_a = mix64(
            seed1 ^ seed2 ^ 0xDEADBEEFCAFEBABEULL);
    const std::uint64_t part_b = mix64(
            (part_a >> 11) ^ (seed2 << 7) ^ 0x0BADC0DE0BADC0DEULL);
    return {part_a, part_b};
}

constexpr std::uint64_t key_a(int line, int counter, const char* file) {
    return make_key(line, counter, file).a;
}

constexpr std::uint64_t key_b(int line, int counter, const char* file) {
    return make_key(line, counter, file).b;
}

constexpr std::array<std::uint32_t, 4> expand_key(const key128& key) {
    return {{
        static_cast<std::uint32_t>(mix64(key.a)),
        static_cast<std::uint32_t>(mix64(key.a ^ key.b) >> 32),
        static_cast<std::uint32_t>(mix64(key.b)),
        static_cast<std::uint32_t>(mix64(key.b ^ 0xa5a5a5a55a5a5a5aULL) >> 32)
    }};
}

constexpr std::uint64_t mix_key_length(const key128& key, size_type length) {
    return mix64(key.a ^ (key.b << 1) ^
            (static_cast<std::uint64_t>(length) * 0xD6E8FEB86659FD93ULL));
}

constexpr std::uint8_t pad_byte(std::uint64_t seed, size_type index) {
    std::uint64_t value = mix64(seed +
            static_cast<std::uint64_t>(index) * 0x9e3779b97f4a7c15ULL);
    return static_cast<std::uint8_t>(value >> ((index & 7) * 8));
}

constexpr std::uint8_t stream_byte(std::uint64_t seed, size_type index) {
    std::uint64_t value = mix64(seed ^
            (static_cast<std::uint64_t>(index + 1) * 0x94d049bb133111ebULL));
    return static_cast<std::uint8_t>(value >> (((index + 3) & 7) * 8));
}

constexpr void encrypt_pass(std::uint8_t* data, size_type length, const key128& key) {
    const std::uint64_t seed = mix_key_length(key, length) ^ 0xC3A5C85C97CB3127ULL;
    for (size_type i = 0; i < length; ++i) {
        std::uint8_t first = pad_byte(seed, i);
        std::uint8_t second = stream_byte(seed, i);
        unsigned rotation = static_cast<unsigned>((first ^ second ^
                static_cast<std::uint8_t>(i)) & 7u);
        if (rotation == 0u) {
            rotation = 5u;
        }
        data[i] = rotate_left8(static_cast<std::uint8_t>((data[i] ^ first) + second), rotation);
    }
}

constexpr void decrypt_pass(std::uint8_t* data, size_type length, const key128& key) {
    const std::uint64_t seed = mix_key_length(key, length) ^ 0xC3A5C85C97CB3127ULL;
    for (size_type i = 0; i < length; ++i) {
        std::uint8_t first = pad_byte(seed, i);
        std::uint8_t second = stream_byte(seed, i);
        unsigned rotation = static_cast<unsigned>((first ^ second ^
                static_cast<std::uint8_t>(i)) & 7u);
        if (rotation == 0u) {
            rotation = 5u;
        }
        data[i] = static_cast<std::uint8_t>((rotate_right8(data[i], rotation) - second) ^ first);
    }
}

constexpr std::array<std::uint32_t, 256> make_crc_table() {
    std::array<std::uint32_t, 256> table{};
    for (std::uint32_t i = 0; i < 256; ++i) {
        std::uint32_t crc = i;
        for (int bit = 0; bit < 8; ++bit) {
            crc = (crc >> 1) ^ ((crc & 1u) * 0xEDB88320u);
        }
        table[i] = crc;
    }
    return table;
}

constexpr std::uint32_t crc32(const std::uint8_t* data, size_type length) {
    constexpr std::array<std::uint32_t, 256> table = make_crc_table();
    std::uint32_t crc = 0xFFFFFFFFu;
    for (size_type i = 0; i < length; ++i) {
        crc = table[(crc ^ data[i]) & 0xFFu] ^ (crc >> 8);
    }
    return crc ^ 0xFFFFFFFFu;
}

constexpr std::uint32_t keyed_hash(const std::uint8_t* data, size_type length,
        const key128& key) {
    std::uint32_t hash = static_cast<std::uint32_t>(
            mix64(key.a ^ key.b ^ 0x811C9DC5u));
    for (size_type i = 0; i < length; ++i) {
        hash ^= static_cast<std::uint32_t>(data[i]) +
                static_cast<std::uint32_t>(i * 131u);
        hash *= 16777619u;
        hash = rotate_left32(hash, 7) ^
                static_cast<std::uint32_t>(mix64(key.a + i) >> 32);
    }
    return hash;
}

constexpr std::uint64_t read_block(const std::uint8_t* block) {
    return static_cast<std::uint64_t>(block[0]) |
            (static_cast<std::uint64_t>(block[1]) << 8) |
            (static_cast<std::uint64_t>(block[2]) << 16) |
            (static_cast<std::uint64_t>(block[3]) << 24) |
            (static_cast<std::uint64_t>(block[4]) << 32) |
            (static_cast<std::uint64_t>(block[5]) << 40) |
            (static_cast<std::uint64_t>(block[6]) << 48) |
            (static_cast<std::uint64_t>(block[7]) << 56);
}

constexpr void write_block(std::uint8_t* block, std::uint64_t value) {
    block[0] = static_cast<std::uint8_t>(value);
    block[1] = static_cast<std::uint8_t>(value >> 8);
    block[2] = static_cast<std::uint8_t>(value >> 16);
    block[3] = static_cast<std::uint8_t>(value >> 24);
    block[4] = static_cast<std::uint8_t>(value >> 32);
    block[5] = static_cast<std::uint8_t>(value >> 40);
    block[6] = static_cast<std::uint8_t>(value >> 48);
    block[7] = static_cast<std::uint8_t>(value >> 56);
}

constexpr void tea_encrypt(std::uint8_t block[8], const std::uint32_t* key) {
    std::uint32_t first = static_cast<std::uint32_t>(block[0]) |
            (static_cast<std::uint32_t>(block[1]) << 8) |
            (static_cast<std::uint32_t>(block[2]) << 16) |
            (static_cast<std::uint32_t>(block[3]) << 24);
    std::uint32_t second = static_cast<std::uint32_t>(block[4]) |
            (static_cast<std::uint32_t>(block[5]) << 8) |
            (static_cast<std::uint32_t>(block[6]) << 16) |
            (static_cast<std::uint32_t>(block[7]) << 24);
    std::uint32_t sum = 0;
    for (int round = 0; round < 32; ++round) {
        first += (((second << 4) ^ (second >> 5)) + second) ^
                (sum + key[sum & 3u]);
        sum += 0x9E3779B9u;
        second += (((first << 4) ^ (first >> 5)) + first) ^
                (sum + key[(sum >> 11) & 3u]);
    }
    block[0] = static_cast<std::uint8_t>(first);
    block[1] = static_cast<std::uint8_t>(first >> 8);
    block[2] = static_cast<std::uint8_t>(first >> 16);
    block[3] = static_cast<std::uint8_t>(first >> 24);
    block[4] = static_cast<std::uint8_t>(second);
    block[5] = static_cast<std::uint8_t>(second >> 8);
    block[6] = static_cast<std::uint8_t>(second >> 16);
    block[7] = static_cast<std::uint8_t>(second >> 24);
}

constexpr void tea_decrypt(std::uint8_t block[8], const std::uint32_t* key) {
    std::uint32_t first = static_cast<std::uint32_t>(block[0]) |
            (static_cast<std::uint32_t>(block[1]) << 8) |
            (static_cast<std::uint32_t>(block[2]) << 16) |
            (static_cast<std::uint32_t>(block[3]) << 24);
    std::uint32_t second = static_cast<std::uint32_t>(block[4]) |
            (static_cast<std::uint32_t>(block[5]) << 8) |
            (static_cast<std::uint32_t>(block[6]) << 16) |
            (static_cast<std::uint32_t>(block[7]) << 24);
    std::uint32_t sum = 0xC6EF3720u;
    for (int round = 0; round < 32; ++round) {
        second -= (((first << 4) ^ (first >> 5)) + first) ^
                (sum + key[(sum >> 11) & 3u]);
        sum -= 0x9E3779B9u;
        first -= (((second << 4) ^ (second >> 5)) + second) ^
                (sum + key[sum & 3u]);
    }
    block[0] = static_cast<std::uint8_t>(first);
    block[1] = static_cast<std::uint8_t>(first >> 8);
    block[2] = static_cast<std::uint8_t>(first >> 16);
    block[3] = static_cast<std::uint8_t>(first >> 24);
    block[4] = static_cast<std::uint8_t>(second);
    block[5] = static_cast<std::uint8_t>(second >> 8);
    block[6] = static_cast<std::uint8_t>(second >> 16);
    block[7] = static_cast<std::uint8_t>(second >> 24);
}

constexpr void tea_cbc_encrypt(std::uint8_t* data, size_type length,
        const key128& key) {
    const std::array<std::uint32_t, 4> words = expand_key(key);
    std::uint64_t previous = mix64(key.a ^ (key.b >> 3) ^
            static_cast<std::uint64_t>(length));
    for (size_type block_index = 0; block_index < length / 8; ++block_index) {
        std::uint8_t* block = data + block_index * 8;
        write_block(block, read_block(block) ^ previous);
        tea_encrypt(block, words.data());
        previous = read_block(block) ^ mix64(previous + block_index + 1);
    }
}

constexpr void tea_cbc_decrypt(std::uint8_t* data, size_type length,
        const key128& key) {
    const std::array<std::uint32_t, 4> words = expand_key(key);
    std::uint64_t previous = mix64(key.a ^ (key.b >> 3) ^
            static_cast<std::uint64_t>(length));
    for (size_type block_index = 0; block_index < length / 8; ++block_index) {
        std::uint8_t* block = data + block_index * 8;
        const std::uint64_t cipher = read_block(block);
        tea_decrypt(block, words.data());
        write_block(block, read_block(block) ^ previous);
        previous = cipher ^ mix64(previous + block_index + 1);
    }
}

inline void xor_dynamic_key(std::uint8_t* data, size_type length) {
    const std::uint64_t key = runtime::get_dynamic_key();
    for (size_type i = 0; i < length; ++i) {
        data[i] ^= static_cast<std::uint8_t>(key >> ((i & 7u) * 8u));
    }
}

}

template <size_type N, std::uint64_t KEY_A, std::uint64_t KEY_B>
class obfuscator {
public:
    constexpr explicit obfuscator(const char* value) {
        static_assert(N > 0, "Obfuscated strings must include a terminator");
        constexpr size_type padded_size = ((N + 7) / 8) * 8;
        std::uint8_t work[padded_size] = {};
        const detail::key128 key = {KEY_A, KEY_B};
        for (size_type i = 0; i < N; ++i) {
            work[i] = static_cast<std::uint8_t>(value[i]);
        }
        const std::uint64_t pad_seed = detail::mix64(
                key.a ^ key.b ^ static_cast<std::uint64_t>(N));
        for (size_type i = N; i < padded_size; ++i) {
            work[i] = detail::pad_byte(pad_seed, i);
        }
        m_plain_hash = detail::keyed_hash(work, N, key);
        detail::encrypt_pass(work, padded_size, key);
        detail::tea_cbc_encrypt(work, padded_size, key);
        for (size_type i = 0; i < padded_size; ++i) {
            m_data[i] = work[i];
        }
        m_checksum = detail::crc32(m_data, padded_size);
    }

    constexpr const std::uint8_t* data() const {
        return m_data;
    }

    constexpr std::uint32_t checksum() const {
        return m_checksum;
    }

    constexpr std::uint32_t plain_hash() const {
        return m_plain_hash;
    }

    constexpr size_type uP6x8Vd() const {
        return sizeof(m_data);
    }

    constexpr std::uint32_t sM1c9Ke() const {
        return m_checksum;
    }

    constexpr std::uint32_t zA0y4Pr() const {
        return m_plain_hash;
    }

private:
    std::uint8_t m_data[((N + 7) / 8) * 8]{};
    std::uint32_t m_checksum{};
    std::uint32_t m_plain_hash{};
};

template <size_type N, std::uint64_t KEY_A, std::uint64_t KEY_B>
class obfuscated_data {
public:
    class access {
    public:
        explicit access(obfuscated_data* owner)
                : owner_(owner), lock_(owner->m_mutex) {
            if (owner_ != nullptr) {
                owner_->decrypt_locked();
            }
        }

        access(const access&) = delete;
        access& operator=(const access&) = delete;

        access(access&& other) noexcept
                : owner_(other.owner_), lock_(std::move(other.lock_)) {
            other.owner_ = nullptr;
        }

        access& operator=(access&&) = delete;

        ~access() {
            if (owner_ != nullptr && !owner_->m_broken && !owner_->m_encrypted) {
                owner_->encrypt_locked();
            }
        }

        const char* get() const {
            if (owner_ == nullptr || owner_->m_broken) {
                return "";
            }
            return reinterpret_cast<const char*>(owner_->m_data);
        }

        operator const char*() const {
            return get();
        }

    private:
        obfuscated_data* owner_;
        std::unique_lock<std::mutex> lock_;
    };

    explicit obfuscated_data(const obfuscator<N, KEY_A, KEY_B>& source)
            : m_encrypted(true), m_broken(false) {
        constexpr size_type data_size = ((N + 7) / 8) * 8;
        for (size_type i = 0; i < data_size; ++i) {
            m_data[i] = source.data()[i];
        }
        m_plain_hash = source.plain_hash();
        detail::xor_dynamic_key(m_data, sizeof(m_data));
        m_stored_checksum = detail::crc32(m_data, sizeof(m_data));
    }

    ~obfuscated_data() {
        std::lock_guard<std::mutex> lock(m_mutex);
        secure_zero();
        m_encrypted = false;
    }

    obfuscated_data(const obfuscated_data&) = delete;
    obfuscated_data& operator=(const obfuscated_data&) = delete;

    access secure_get() {
        return access(this);
    }

    access acquire() {
        return access(this);
    }

    void rB2u7Wt() {
        std::lock_guard<std::mutex> lock(m_mutex);
        if (!m_encrypted && !m_broken) {
            encrypt_locked();
        }
    }

    bool eH9b2Tf() const {
        std::lock_guard<std::mutex> lock(m_mutex);
        return m_encrypted;
    }

private:
    void secure_zero() {
        volatile std::uint8_t* pointer = m_data;
        for (size_type i = 0; i < sizeof(m_data); ++i) {
            pointer[i] = 0;
        }
    }

    void mark_broken() {
        secure_zero();
        m_broken = true;
        m_encrypted = false;
    }

    void decrypt_locked() {
        if (!m_encrypted || m_broken) {
            return;
        }
        if (detail::crc32(m_data, sizeof(m_data)) != m_stored_checksum) {
            mark_broken();
            return;
        }
        const detail::key128 key = {KEY_A, KEY_B};
        detail::xor_dynamic_key(m_data, sizeof(m_data));
        detail::tea_cbc_decrypt(m_data, sizeof(m_data), key);
        detail::decrypt_pass(m_data, sizeof(m_data), key);
        if (detail::keyed_hash(m_data, N, key) != m_plain_hash || m_data[N - 1] != 0) {
            mark_broken();
            return;
        }
        m_encrypted = false;
    }

    void encrypt_locked() {
        if (m_encrypted || m_broken) {
            return;
        }
        const detail::key128 key = {KEY_A, KEY_B};
        if (detail::keyed_hash(m_data, N, key) != m_plain_hash || m_data[N - 1] != 0) {
            mark_broken();
            return;
        }
        detail::encrypt_pass(m_data, sizeof(m_data), key);
        detail::tea_cbc_encrypt(m_data, sizeof(m_data), key);
        detail::xor_dynamic_key(m_data, sizeof(m_data));
        m_stored_checksum = detail::crc32(m_data, sizeof(m_data));
        m_encrypted = true;
    }

    std::uint8_t m_data[((N + 7) / 8) * 8]{};
    std::uint32_t m_stored_checksum{};
    std::uint32_t m_plain_hash{};
    bool m_encrypted;
    bool m_broken;
    mutable std::mutex m_mutex;
};

template <size_type N, std::uint64_t KEY_A, std::uint64_t KEY_B>
constexpr obfuscator<N, KEY_A, KEY_B> bL6s4Cv(const char (&value)[N]) {
    return obfuscator<N, KEY_A, KEY_B>(value);
}

inline void initialize_obfuscator() {
    runtime::initialize();
}

}

#define AY_OBFUSCATE_DEFAULT_KEY_A \
    ay::detail::key_a(__LINE__, __COUNTER__, __FILE__)
#define AY_OBFUSCATE_DEFAULT_KEY_B \
    ay::detail::key_b(__LINE__, __COUNTER__, __FILE__)

#define OBFUSCATE_KEY(data, keyA, keyB) \
    ([]() { \
        constexpr ay::size_type ay_obf_size = sizeof(data) / sizeof(data[0]); \
        constexpr auto ay_obf_value = ay::bL6s4Cv<ay_obf_size, keyA, keyB>(data); \
        static ay::obfuscated_data<ay_obf_size, keyA, keyB> ay_obf_instance(ay_obf_value); \
        return ay_obf_instance.acquire(); \
    }())

#define OBFUSCATE(data) \
    OBFUSCATE_KEY(data, AY_OBFUSCATE_DEFAULT_KEY_A, AY_OBFUSCATE_DEFAULT_KEY_B)

#define OBFUSCATE_TEMP(data) \
    OBFUSCATE(data)

#define OBFUSCATE_STRING_KEY(data, keyA, keyB) \
    OBFUSCATE_KEY(data, keyA, keyB)

#define OBFUSCATE_STRING(data) \
    OBFUSCATE_STRING_KEY(data, AY_OBFUSCATE_DEFAULT_KEY_A, AY_OBFUSCATE_DEFAULT_KEY_B)

#endif
