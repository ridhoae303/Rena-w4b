#include <cstddef>
#include <cstdint>
#include <string>
#include <type_traits>
#include <utility>

#ifndef AY_NOINLINE
#    if defined(__clang__) || defined(__GNUC__)
#        define AY_NOINLINE __attribute__((noinline))
#    else
#        define AY_NOINLINE
#    endif
#endif

#ifndef AY_OBFUSCATE_DEFAULT_KEY
#    define AY_OBFUSCATE_DEFAULT_KEY ay::generate_key(__FILE__, __LINE__, __COUNTER__)
#endif

namespace ay {

using size_type = unsigned long long;
using key_type  = unsigned long long;

constexpr key_type rotl64(key_type x, unsigned int r) {
    return (x << r) | (x >> (64u - r));
}

constexpr key_type mix64(key_type x) {
    x ^= x >> 30;
    x *= 0xbf58476d1ce4e5b9ULL;
    x ^= x >> 27;
    x *= 0x94d049bb133111ebULL;
    x ^= x >> 31;
    return x;
}

constexpr key_type hash_file(const char* text, size_type length) {
    key_type h = 0xcbf29ce484222325ULL;
    for (size_type i = 0; i < length; ++i) {
        h ^= static_cast<unsigned char>(text[i]);
        h *= 0x100000001b3ULL;
    }
    return h;
}

constexpr key_type string_length(const char* text) {
    size_type n = 0;
    while (text[n] != '\0') ++n;
    return n;
}

constexpr key_type generate_key(
        const char* file,
        size_type line,
        size_type counter) {
    const key_type fileHash = hash_file(file, string_length(file));
    const key_type seed =
            fileHash ^
            (line * 0x9e3779b97f4a7c15ULL) ^
            (counter * 0xd6e8feb86659fd93ULL);
    key_type key = mix64(seed + 0xa5b35705c8f2d9a1ULL);
    key ^= rotl64(mix64(seed ^ 0x3c6ef372fe94f82bULL), 23);
    key ^= 0x0101010101010101ULL;
    return key | 0x0101010101010101ULL;
}

constexpr unsigned char key_byte(key_type key, size_type index, unsigned int lane) {
    const key_type laneSeed =
            key ^
            (static_cast<key_type>(index + 1) * 0x9e3779b97f4a7c15ULL) ^
            (static_cast<key_type>(lane + 1) * 0xd6e8feb86659fd93ULL);
    const key_type mixed = mix64(laneSeed + 0x6a09e667f3bcc909ULL);
    return static_cast<unsigned char>(mixed >> ((lane & 7u) * 8u));
}

constexpr unsigned char rotl8(unsigned char value, unsigned int shift) {
    shift &= 7u;
    if (shift == 0) return value;
    return static_cast<unsigned char>(
            static_cast<unsigned char>(value << shift) |
            static_cast<unsigned char>(value >> (8u - shift)));
}

constexpr unsigned char rotr8(unsigned char value, unsigned int shift) {
    shift &= 7u;
    if (shift == 0) return value;
    return static_cast<unsigned char>(
            static_cast<unsigned char>(value >> shift) |
            static_cast<unsigned char>(value << (8u - shift)));
}

constexpr unsigned char encrypt_byte(
        unsigned char value,
        key_type key,
        size_type index) {
    const unsigned char k0 = key_byte(key, index, 0);
    const unsigned char k1 = key_byte(key, index, 1);
    const unsigned char k2 = key_byte(key, index, 2);
    const unsigned char k3 = key_byte(key, index, 3);
    unsigned char out = static_cast<unsigned char>(value ^ k0);
    out = rotl8(out, static_cast<unsigned int>((k1 & 7u) + 1u));
    out = static_cast<unsigned char>(out + k2);
    out = static_cast<unsigned char>(out ^ k3);
    return out;
}

constexpr unsigned char decrypt_byte(
        unsigned char value,
        key_type key,
        size_type index) {
    const unsigned char k0 = key_byte(key, index, 0);
    const unsigned char k1 = key_byte(key, index, 1);
    const unsigned char k2 = key_byte(key, index, 2);
    const unsigned char k3 = key_byte(key, index, 3);
    unsigned char out = static_cast<unsigned char>(value ^ k3);
    out = static_cast<unsigned char>(out - k2);
    out = rotr8(out, static_cast<unsigned int>((k1 & 7u) + 1u));
    out = static_cast<unsigned char>(out ^ k0);
    return out;
}

template <size_type N, key_type KEY>
class obfuscator {
public:
    constexpr obfuscator(const char* data) {
        for (size_type i = 0; i < N; ++i) {
            m_data[i] = static_cast<char>(
                    encrypt_byte(
                            static_cast<unsigned char>(data[i]),
                            KEY,
                            i));
        }
    }

    constexpr const char* data() const { return m_data; }
    constexpr size_type size() const { return N; }
    constexpr key_type key() const  { return KEY; }

private:
    char m_data[N]{};
};

template <size_type N, key_type KEY>
class obfuscated_data {
public:
    obfuscated_data(const obfuscator<N, KEY>& obf) {
        for (size_type i = 0; i < N; ++i)
            m_data[i] = obf.data()[i];
    }

    ~obfuscated_data() {
        volatile unsigned char* p =
                reinterpret_cast<volatile unsigned char*>(m_data);
        for (size_type i = 0; i < N; ++i)
            p[i] = 0;
        m_encrypted = true;
    }

    AY_NOINLINE operator char*() {
        decrypt();
        return m_data;
    }

    AY_NOINLINE operator std::string() {
        decrypt();
        return std::string(m_data);
    }

    AY_NOINLINE void decrypt() {
        if (m_encrypted) {
            for (size_type i = 0; i < N; ++i) {
                volatile unsigned char* p =
                        reinterpret_cast<volatile unsigned char*>(&m_data[i]);
                *p = decrypt_byte(*p, KEY, i);
            }
            m_encrypted = false;
        }
    }

    AY_NOINLINE void encrypt() {
        if (!m_encrypted) {
            for (size_type i = 0; i < N; ++i) {
                volatile unsigned char* p =
                        reinterpret_cast<volatile unsigned char*>(&m_data[i]);
                *p = encrypt_byte(*p, KEY, i);
            }
            m_encrypted = true;
        }
    }

    bool is_encrypted() const { return m_encrypted; }

    obfuscated_data(const obfuscated_data&) = delete;
    obfuscated_data& operator=(const obfuscated_data&) = delete;

private:
    char m_data[N];
    bool m_encrypted{ true };
};

template <size_type N, key_type KEY = AY_OBFUSCATE_DEFAULT_KEY>
constexpr auto make_obfuscator(const char(&data)[N]) {
    return obfuscator<N, KEY>(data);
}

}

#define OBFUSCATE(data) OBFUSCATE_KEY(data, AY_OBFUSCATE_DEFAULT_KEY)

#define OBFUSCATE_KEY(data, key) \
    []() -> ay::obfuscated_data<sizeof(data)/sizeof(data[0]), key>& { \
        static_assert(sizeof(decltype(key)) == sizeof(ay::key_type), "key must be 64-bit"); \
        constexpr auto n = sizeof(data)/sizeof(data[0]); \
        constexpr auto obf = ay::make_obfuscator<n, key>(data); \
        static auto obf_data = ay::obfuscated_data<n, key>(obf); \
        return obf_data; \
    }()
