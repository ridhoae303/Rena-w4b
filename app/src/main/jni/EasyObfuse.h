#include <cstddef>
#include <string>
#include <type_traits>

#ifndef AY_OBFUSCATE_DEFAULT_KEY
    #define AY_OBFUSCATE_DEFAULT_KEY ay::generate_key(__LINE__)
#endif

namespace ay {

using size_type = unsigned long long;
using key_type  = unsigned long long;

constexpr key_type generate_key(key_type seed) {
    key_type key = seed;
    key ^= (key >> 33);
    key *= 0xff51afd7ed558ccdULL;
    key ^= (key >> 33);
    key *= 0xc4ceb9fe1a85ec53ULL;
    key ^= (key >> 33);
    key += 0x9e3779b97f4a7c15ULL;
    key ^= (key << 27);
    key *= 0x94d049bb133111ebULL;
    key ^= (key >> 31);
    key |= 0x0101010101010101ULL;
    return key;
}

constexpr void stream_cipher(char* data, size_type size, key_type key) {
    key_type s0 = key;
    key_type s1 = key ^ 0x9e3779b97f4a7c15ULL;
    for (size_type i = 0; i < size; ++i) {
        key_type s1_temp = s1;
        s0 = s1_temp;
        s1 ^= (s1 << 23);
        s1 ^= (s0 >> 18);
        s1 ^= s0;
        s1 ^= (s0 >> 5);
        key_type keystream_byte = (s0 + s1) >> ((i % 8) * 8);
        data[i] ^= static_cast<char>(keystream_byte);
    }
}

template <size_type N, key_type KEY>
class obfuscator {
public:
    constexpr obfuscator(const char* data) {
        for (size_type i = 0; i < N; ++i)
            m_data[i] = data[i];
        stream_cipher(m_data, N, KEY);
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
        volatile char* p = m_data;
        for (size_type i = 0; i < N; ++i)
            p[i] = 0;
        m_encrypted = true;
    }
    operator char*() {
        decrypt();
        return m_data;
    }
    operator std::string() {
        decrypt();
        return std::string(m_data);
    }
    void decrypt() {
        if (m_encrypted) {
            stream_cipher(m_data, N, KEY);
            m_encrypted = false;
        }
    }
    void encrypt() {
        if (!m_encrypted) {
            stream_cipher(m_data, N, KEY);
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
        static_assert((key) >= (1ull << 56), "key must span all 8 bytes"); \
        constexpr auto n = sizeof(data)/sizeof(data[0]); \
        constexpr auto obf = ay::make_obfuscator<n, key>(data); \
        static auto obf_data = ay::obfuscated_data<n, key>(obf); \
        return obf_data; \
    }()
