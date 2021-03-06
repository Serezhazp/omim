#include "drape_frontend/memory_feature_index.hpp"

namespace df
{

void MemoryFeatureIndex::ReadFeaturesRequest(vector<FeatureInfo> & features, vector<size_t> & indexes)
{
  threads::MutexGuard lock(m_mutex);

  for (size_t i = 0; i < features.size(); ++i)
  {
    FeatureInfo & info = features[i];
    ASSERT(m_features.find(info.m_id) != m_features.end() || !info.m_isOwner,());
    if (!info.m_isOwner && m_features.insert(info.m_id).second)
    {
      indexes.push_back(i);
      info.m_isOwner = true;
    }
  }
}

void MemoryFeatureIndex::RemoveFeatures(vector<FeatureInfo> & features)
{
  threads::MutexGuard lock(m_mutex);

  for (size_t i = 0; i < features.size(); ++i)
  {
    FeatureInfo & info = features[i];
    if (info.m_isOwner)
    {
      VERIFY(m_features.erase(info.m_id) == 1, ());
      info.m_isOwner = false;
    }
  }
}

} // namespace df
