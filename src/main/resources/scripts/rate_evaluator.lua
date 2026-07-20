-- Keys: [1] rate_limit_key
-- Args: [1] max_capacity, [2] refill_rate_per_sec, [3] current_timestamp_sec, [4] requested_tokens

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Retrieve current bucket state
local data = redis.call("hmget", key, "tokens", "last_updated")
local tokens = tonumber(data[1])
local last_updated = tonumber(data[2])

-- Initialize bucket if it doesn't exist
if not tokens then
    tokens = capacity
    last_updated = now
else
    -- Calculate token replenishment based on elapsed time delta
    local elapsed = math.max(0, now - last_updated)
    tokens = math.min(capacity, tokens + (elapsed * refill_rate))
end

-- Evaluate request criteria
if tokens >= requested then
    tokens = tokens - requested
    redis.call("hmset", key, "tokens", tokens, "last_updated", now)
    redis.call("expire", key, 3600) -- Auto-evict stale client tracking after 1hr
    return 1 -- Allowed
else
    return 0 -- Rate limited (HTTP 429 Drop)
end
